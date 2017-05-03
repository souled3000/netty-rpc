package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0012;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0013;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0014;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0015;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0016;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0017;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.common.PBKDF2;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

import redis.clients.jedis.Jedis;

@Controller("/step2")
public class UserFindPwdStep2Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdStep2Api.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String userEmail = req.getParameter("email");
		String code = req.getParameter("code");
		String keyCode = new String(userEmail + "-code");
		String pwd = req.getParameter("pwd");

		logger.info("UserFindPwdStep2Handler begin userEmail:{}|code:{}|keyCode:{}|pwd:{}", userEmail, code, keyCode, pwd);

		if (StringUtils.isBlank(pwd)) {
			r.put(status, C0012.toString());
			logger.info("pwd is null. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail, code, keyCode, pwd, r.get(status));
			return r;
		}
		if (StringUtils.isBlank(code)) {
			r.put(status, C0013.toString());
			logger.info("code is null. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail, code, keyCode, pwd, r.get(status));
			return r;
		}
		if (StringUtils.isBlank(userEmail)) {
			r.put(status, C0014.toString());
			logger.info("userEmail is null. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail, code, keyCode, pwd, r.get(status));
			return r;
		}
		Jedis jedis = null;
		try {
			jedis = JedisHelper.getJedis();
			// 验证码是否过期
			if (!jedis.exists(keyCode)) {
				r.put(status, C0015.toString());
				logger.info("code has been expired. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail, code, keyCode, pwd, r.get(status));
				return r;
			}

			// 超三次验证失败直接返回
			String codeVal = jedis.get(keyCode);
			String strFailTime = jedis.get(keyCode + "fail");
			int failTime = Integer.valueOf(strFailTime == null ? "0" : strFailTime);
			if (failTime >= 3) {
				r.put(status, C0016.toString());
				logger.info("the times validating beyond three. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail, code, keyCode, pwd, r.get(status));
				return r;
			}
			// 验证
			if (!codeVal.equals(code)) {
				jedis.incr(keyCode + "fail");// 累记失败次数
				r.put(status, C0017.toString());
				logger.info("validating fail. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail, code, keyCode, pwd, r.get(status));
				return r;
			}
			String userId = jedis.hget("user:mailtoid", userEmail);

			// 生成新密码
			String newShadow = PBKDF2.encode(pwd);
			jedis.hset("user:shadow", userId, newShadow);
			jedis.publish("PubModifiedPasswdUser", userId);

		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(jedis);
			logger.error("", e);
			return r;
		} finally {
			JedisHelper.returnJedis(jedis);
		}
		logger.info("response: userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail, code, keyCode, pwd, r.get(status));
		r.put(status, SUCCESS.toString());
		return r;
	}
}