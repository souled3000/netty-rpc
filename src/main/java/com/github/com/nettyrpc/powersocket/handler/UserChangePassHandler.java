package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.user.UserChangePassResponse;
import com.github.com.nettyrpc.util.HttpUtil;
import com.github.com.nettyrpc.util.PBKDF2;

public class UserChangePassHandler implements IHandler {
	private static final Logger logger = LoggerFactory
			.getLogger(UserChangePassHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		UserChangePassResponse result = new UserChangePassResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String passOld = HttpUtil.getPostValue(req.getParams(), "passOld");
		String passNew = HttpUtil.getPostValue(req.getParams(), "passNew");

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 用户密码
			String shadow = jedis.hget("user:shadow", userId);
			if (null == shadow) {
				result.setStatusMsg("userId not exist.");
				result.setStatus(1);
				return result;
			}

			// 2. 校验密码是否正确
			if (!PBKDF2.validate(passOld, shadow)) {
				result.setStatusMsg("Password is incorrect.");
				result.setStatus(2);
				return result;
			}

			// 3. 生成新密码
			String newShadow = PBKDF2.encode(passNew);
			jedis.hset("user:shadow", userId, newShadow);

			result.setStatus(0);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("User change password error");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;

	}

}
