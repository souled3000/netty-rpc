package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

/**
 * 绑定手机号码的第一步，发送短信验证码。
 * 
 * @author j
 * 
 */
@Controller("/mobile/phonebind/step1")
public class PhoneBindStep1Api extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(PhoneBindStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	private static final int DO_INTV_TTL = Integer.valueOf(Constants.getProperty("phonebind.step1.interval.ttl", "60"));

	private static final int DO_FREQ_TTL = Integer.valueOf(Constants.getProperty("phonebind.step1.frequency.ttl", "86400"));

	private static final int DO_FREQ_MAX = Integer.valueOf(Constants.getProperty("phonebind.step1.frequency.max", "5"));

	public static final String CODE_KEY = "ttl:user:phonebind:step1:";

	public static final String INTV_KEY = "ttl:user:phonebind:step1:interval:";

	public static final String FREQ_KEY = "ttl:user:phonebind:step1:frequency:";

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("status", SYSERROR.toString());

		// 入参解析：cookie， phone
		String cookie = req.getParameter("cookie");
		String phone = req.getParameter("phone");

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
			ret.put("status", "用户没找到");
			return ret;
		}

		// 用户已经绑定手机号码？
		String phoneable = user.getPhoneable();
		if ("true".equalsIgnoreCase(phoneable)) {
			ret.put("status", "已绑定手机号码，请勿重复绑定");
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			Transaction trans = jedis.multi();

			// 发送验证码次数是否太频繁，是否超限？
			String interV = "";
			String frequV = "";

			interV = jedis.get(INTV_KEY);
			frequV = jedis.get(FREQ_KEY);

			if (StringUtils.isNotBlank(interV)) {
				ret.put("status", "频繁操作,请稍后再试");
				return ret;
			}

			if (StringUtils.isNotBlank(frequV)) {
				if (Integer.valueOf(frequV) >= DO_FREQ_MAX) {
					ret.put("status", "已达操作上限,请稍后再试");
					return ret;
				}
			}

			// 生成验证码，服务器端临时存储
			String code = VerifyCode.randString(CODE_LENGTH);
			trans.setex(CODE_KEY + phone, CODE_EXPIRE, code);

			// 发送验证码是否成功？
			SMSSender.send(phone, "验证码【" + code + "】");

			// 更新状态记录
			interV = "1";
			frequV = String.valueOf(Integer.valueOf(frequV) + 1);

			trans.setex(INTV_KEY + phone, DO_INTV_TTL, interV);
			trans.setex(FREQ_KEY + phone, DO_FREQ_TTL, frequV);

			// 提交
			trans.exec();

			// 返回
			ret.put("status", SUCCESS.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
