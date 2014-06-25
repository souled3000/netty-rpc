package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.user.UserRegisterResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;
import com.github.com.nettyrpc.util.PBKDF2;

public class UserRegisterHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UserRegisterHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		UserRegisterResponse result = new UserRegisterResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String email = HttpUtil.getPostValue(req.getParams(), "email");
		String phone = HttpUtil.getPostValue(req.getParams(), "phone");
		String passwd = HttpUtil.getPostValue(req.getParams(), "passwd");

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 邮箱是否注册
			String existId = jedis.hget("user:mailtoid", email);
			if (null != existId) {
				result.setStatusMsg("Regist failed! mail exists.");
				return result;
			}

			// 2. 生成用户ID
			String userId = String.valueOf(jedis.incr("user:nextid"));

			Transaction tx = jedis.multi();

			// 3. 记录<邮箱，用户Id>
			tx.hset("user:mailtoid", email, userId);

			// 4. 记录<用户Id，邮箱>
			tx.hset("user:email", userId, email);

			// 5. 记录<用户Id，电话号码>
			tx.hset("user:phone", userId, phone);

			// 6. 记录<用户Id，密码>
			String shadow = PBKDF2.encode(passwd);
			tx.hset("user:shadow", userId, shadow);

			result.setStatus(0);
			result.setUrlOrigin(req.getUrlOrigin());

			String cookie = CookieUtil.encode(userId, CookieUtil.EXPIRE_SEC);
			String timeStamp = String.valueOf(System.currentTimeMillis());
			String proxyKey = CookieUtil.generateKey(userId, timeStamp,
					CookieUtil.EXPIRE_SEC);
			String proxyAddr = CookieUtil.getWebsocketAddr();

			result.setUserId(userId);
			result.setCookie(cookie);
			result.setProxyKey(proxyKey);
			result.setProxyAddr(proxyAddr);

			tx.exec();
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			String msg = String.format("User regist error, msg: %s",
					e.getMessage());
			logger.error(msg);
			throw new InternalException(msg);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;

	}

}
