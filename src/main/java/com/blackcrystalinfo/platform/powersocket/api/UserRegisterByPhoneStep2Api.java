package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
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
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.VerifyCode;

/**
 * 手机号码注册第二步：入库
 * 
 * @author j
 * 
 */
@Controller("/registerbyphone/step2")
public class UserRegisterByPhoneStep2Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserRegisterByPhoneStep1Api.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("status", SYSERROR.toString());

		String phone = req.getParameter("phone");
		String code = req.getParameter("code");

		if (StringUtils.isBlank(phone)) {
			ret.put("status", "手机号码不可以为空");
			return ret;
		}

		if (StringUtils.isBlank(code)) {
			ret.put("status", "验证码不可以为空");
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			String key = "test:tmp:registebyphone:step1:" + phone;
			String value = jedis.get(key);
			if (StringUtils.isBlank(value)) {
				ret.put("status", "请先获取短信验证码");
				return ret;
			}

			if (!StringUtils.equals(value, code)) {
				ret.put("status", "短信验证码输入错误，请重新输入。");
				return ret;
			}

			String id = jedis.hget("test:user:phone2id", phone);
			if (StringUtils.isNotBlank(id)) {
				ret.put("status", "手机已注册，请直接登录");
				return ret;
			}

			Long userId = jedis.incr("test:user:nextId");
			String userKey = "test:user:" + userId;

			String username = phone;
			String password = VerifyCode.randString(10);

			jedis.hset(userKey, "username", username);
			jedis.hset(userKey, "password", password);

			jedis.hset("test:user:phone2id", phone, userId.toString());

			ret.put("userId", userId);
			ret.put("username", username);
			ret.put("password", password);

			ret.put("status", SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
