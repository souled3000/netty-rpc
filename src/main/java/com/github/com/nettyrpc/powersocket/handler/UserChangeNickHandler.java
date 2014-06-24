package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.user.UserChangeNickResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

/**
 * 修改用户昵称
 * 
 * @author j
 * 
 */
public class UserChangeNickHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UserChangeNickHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		logger.info("request: {}", req);

		UserChangeNickResponse result = new UserChangeNickResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String nick = HttpUtil.getPostValue(req.getParams(), "nick");

		// 1. 校验cookie信息
		String[] infos = null;
		try {
			infos = CookieUtil.decode(cookie);

			if (!userId.equals(infos[0])) {
				throw new IllegalArgumentException();
			}
		} catch (Exception e) {
			logger.error("Cookie decode error.");
			result.setStatusMsg("Cookie decode error.");
			return result;
		}

		// 2. 修改昵称
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String oldNick = jedis.hget("user:nick", userId);
			if (!nick.equals(oldNick)) {
				// 新旧Nick不一致时修改
				jedis.hset("user:nick", userId, nick);
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("User change nick error");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}

}
