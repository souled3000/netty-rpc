package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.DateUtils;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.VerifyCode;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

/**
 * 手机号码注册第一步：发送短信验证码
 * 
 * @author j
 * 
 */
@Controller("/registerbyphone/step1")
public class UserRegisterByPhoneStep1Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserRegisterByPhoneStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("status", SYSERROR.toString());

		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			ret.put("status", "手机号码不可以为空");
			return ret;
		}

		Jedis jedis = null;

		try {
			jedis = DataHelper.getJedis();

			String id = jedis.hget("test:user:phone2id", phone);
			if (StringUtils.isNotBlank(id)) {
				ret.put("status", "手机已注册，请直接登录");
				return ret;
			}

			String key = "test:tmp:registebyphone:step1:" + phone;
			String value = jedis.get(key);
			if (StringUtils.isNotBlank(value)) {
				ret.put("status", "发送太频繁了，请" + DateUtils.secToTime(CODE_EXPIRE) + "后重试！");
				return ret;
			}

			// send message
			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put("status", "发送短信验证码失败！");
				return ret;
			}

			// 记录短信验证码
			jedis.setex(key, CODE_EXPIRE, code);

			ret.put("code", code);
			ret.put("status", ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
