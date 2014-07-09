package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.user.UserLoginResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;
import com.github.com.nettyrpc.util.PBKDF2;

public class UserLoginHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UserLoginHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		UserLoginResponse result = new UserLoginResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String email = HttpUtil.getPostValue(req.getParams(), "email");
		String passwd = HttpUtil.getPostValue(req.getParams(), "passwd");

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 根据Email获取userId
			String userId = jedis.hget("user:mailtoid", email);
			if (null == userId) {
				result.setStatusMsg("User not exist.");
				return result;
			}

			// 2. encodePwd与passwd加密后的串做比较
			String encodePwd = jedis.hget("user:shadow", userId);
			if (!PBKDF2.validate(passwd, encodePwd)) {
				result.setStatusMsg("Password error.");
				return result;
			}
			
			
			String cookie = CookieUtil.encode(userId, CookieUtil.EXPIRE_SEC);
			String proxyKey = CookieUtil.generateKey(userId, String.valueOf(System.currentTimeMillis()/1000), CookieUtil.EXPIRE_SEC);
			String proxyAddr = CookieUtil.getWebsocketAddr();

			result.setStatus(0);
			result.setUserId(userId);
			result.setExpire(CookieUtil.EXPIRE_SEC);
			result.setCookie(cookie);
			result.setProxyKey(proxyKey);
			result.setProxyAddr(proxyAddr);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("User login error.");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}

}
