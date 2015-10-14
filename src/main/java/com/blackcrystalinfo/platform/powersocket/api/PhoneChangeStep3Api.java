package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.VerifyCode;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

/**
 * 修改绑定手机第三步，新手机发送短信验证码
 * 
 * @author j
 * 
 */
@Controller("/mobile/phonechange/step3")
public class PhoneChangeStep3Api extends HandlerAdapter {
	private Logger logger = LoggerFactory.getLogger(PhoneChangeStep3Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	private static final int DO_INTV_TTL = Integer.valueOf(Constants.getProperty("phonechange.step3.interval.ttl", "60"));

	private static final int DO_FREQ_TTL = Integer.valueOf(Constants.getProperty("phonechange.step3.frequency.ttl", "86400"));

	private static final int DO_FREQ_MAX = Integer.valueOf(Constants.getProperty("phonechange.step3.frequency.max", "5"));

	public static final String CODE_KEY = "ttl:user:phonechange:step3:";

	public static final String INTV_KEY = "ttl:user:phonechange:step3:interval:";

	public static final String FREQ_KEY = "ttl:user:phonechange:step3:frequency:";

	public static final String STEP3_KEY = "test:tmp:phonechange:step3key:";

	public static final String STEP3_PHONE = "test:tmp:phonechange:step3phone:";

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR);

		// 入参解析：cookie， phone
		String cookie = req.getParameter("cookie");
		String step2key = req.getParameter("step2key");
		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			ret.put(status, "手机号码不可以为空");
			return ret;
		}

		if (StringUtils.isBlank(step2key)) {
			ret.put(status, "第一步的凭证不可以为空");
			return ret;
		}

		// phone是否格式正确？用户是否存在？
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		User user = null;
		try {
			user = userDao.userGet(User.UserIDColumn, userId);

			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by id.", e);
			ret.put(status, "用户没找到");
			return ret;
		}

		// 新旧手机号一致？
		String oldPhone = user.getPhone();
		if (oldPhone.equals(phone)) {
			ret.put(status, "新手机号与旧手机号一致，请绑定其他手机号");
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第二步凭证
			String step2keyK = PhoneChangeStep2Api.STEP2_KEY + userId;
			String step2keyV = jedis.get(step2keyK);
			if (!StringUtils.equals(step2keyV, step2key)) {
				ret.put(status, "修改绑定手机号码第一步凭证有误");
				return ret;
			}

			// 发送验证码次数是否太频繁，是否超限？
			String interV = "";
			String frequV = "";

			interV = jedis.get(INTV_KEY);
			frequV = jedis.get(FREQ_KEY);

			if (StringUtils.isNotBlank(interV)) {
				ret.put(status, "频繁操作,请稍后再试");
				return ret;
			}

			if (StringUtils.isNotBlank(frequV)) {
				if (Integer.valueOf(frequV) >= DO_FREQ_MAX) {
					ret.put(status, "已达操作上限,请稍后再试");
					return ret;
				}
			} else {
				frequV = "0";
			}

			Transaction trans = jedis.multi();

			// 生成验证码，服务器端临时存储
			String code = VerifyCode.randString(CODE_LENGTH);
			trans.setex(CODE_KEY + userId, CODE_EXPIRE, code);

			// 发送验证码是否成功？
			if (!SMSSender.send(oldPhone, "验证码【" + code + "】")) {
				ret.put(status, "发送验证码失败");
				return ret;
			}
			;

			// 更新状态记录
			interV = "1";
			frequV = String.valueOf(Integer.valueOf(frequV) + 1);

			trans.setex(INTV_KEY + userId, DO_INTV_TTL, interV);
			trans.setex(FREQ_KEY + userId, DO_FREQ_TTL, frequV);

			// 提交
			trans.exec();

			// 生成第三步凭证
			String step3keyK = STEP3_KEY + userId;
			String step3keyV = UUID.randomUUID().toString();
			jedis.setex(step3keyK, CODE_EXPIRE, step3keyV);

			// 临时存储新手机号码
			String step3phoneK = STEP3_PHONE + userId;
			String step3phoneV = phone;
			jedis.setex(step3phoneK, CODE_EXPIRE, step3phoneV);

			// 返回
			ret.put("code", code);
			ret.put("step3key", step3keyV);
			ret.put(status, ErrorCode.SUCCESS);
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
