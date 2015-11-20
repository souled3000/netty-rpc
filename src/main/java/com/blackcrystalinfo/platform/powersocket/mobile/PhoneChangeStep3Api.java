package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0035;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0037;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0038;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0040;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0041;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0044;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

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

	private static final int DO_INTV_TTL = Integer.valueOf(Constants.getProperty("phonechange.step3.interval.ttl", "30"));

	private static final int DO_FREQ_TTL = Integer.valueOf(Constants.getProperty("phonechange.step3.frequency.ttl", "86400"));

	private static final int DO_FREQ_MAX = Integer.valueOf(Constants.getProperty("phonechange.step3.frequency.max", "5"));

	public static final String CODE_KEY = "B0034:";

	public static final String INTV_KEY = "B0034:interval:";

	public static final String FREQ_KEY = "B0034:frequency:";

	public static final String STEP3_KEY = "B0034:step3key:";

	public static final String STEP3_PHONE = "B0034:step3phone:";

	@Autowired
	private IUserSvr userSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR);

		// 入参解析：cookie， phone
		String cookie = req.getParameter("cookie");
		String step2key = req.getParameter("step2key");
		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			ret.put(status, C0035.toString());
			return ret;
		}

		if (StringUtils.isBlank(step2key)) {
			ret.put(status, C0040.toString());
			return ret;
		}

		// phone是否格式正确？用户是否存在？
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		User user = userSvr.getUser(User.UserIDColumn, userId);
		if (null == user) {
			ret.put(status, ErrorCode.C0006.toString());
			return ret;
		}

		// 新旧手机号一致？
		String oldPhone = user.getPhone();
		if (oldPhone.equals(phone)) {
			ret.put(status, C0044.toString());
			return ret;
		}

		// 手机号是否已经注册
		if (userSvr.userExist(phone)) {
			ret.put(status, ErrorCode.C0036.toString());
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第二步凭证
			String step2keyK = PhoneChangeStep2Api.STEP2_KEY + userId;
			String step2keyV = jedis.get(step2keyK);
			if (!StringUtils.equals(step2keyV, step2key)) {
				ret.put(status, C0041.toString());
				return ret;
			}

			// 发送验证码次数是否太频繁，是否超限？
			String interV = "";
			String frequV = "";

			interV = jedis.get(INTV_KEY+userId);
			frequV = jedis.get(FREQ_KEY+userId);

			if (StringUtils.isNotBlank(interV)) {
				ret.put(status, C0037.toString());
				return ret;
			}

			if (StringUtils.isNotBlank(frequV)) {
				if (Integer.valueOf(frequV) >= DO_FREQ_MAX) {
					ret.put(status, C002C.toString());
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
			if (!SMSSender.send(phone, "验证码【" + code + "】")) {
				ret.put(status, C0038.toString());
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
			ret.put("count", frequV);
			ret.put("step3key", step3keyV);
			ret.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
