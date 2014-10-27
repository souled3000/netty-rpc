package com.blackcrystalinfo.platform.powersocket.handler;

import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserChangeNickResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

/**
 * 修改用户昵称
 * 
 * @author j
 * 
 */
public class UserChangeNickHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserChangeNickHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		UserChangeNickResponse result = new UserChangeNickResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String nick = HttpUtil.getPostValue(req.getParams(), "nick");
		logger.info("UserChangeNickHandler begin userId:{}|cookie:{}|nick:{}", userId, cookie, nick);

		String[] cs = cookie.split("-");

		if (cs.length != 2) {
			result.setStatus(3);
			logger.info("UserChangeNick status:{}|cookie:{}", result.getStatus(), cookie);
			return result;
		}

		Jedis jedis = null;
		// 1. 校验cookie信息
		try {
			jedis = DataHelper.getJedis();

			String shadow = jedis.hget("user:shadow", userId);
			String csmd5 = cs[1];
			String csmd52 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId + shadow).getBytes()));

			if (!csmd5.equals(csmd52)) {
				result.setStatus(7);
				logger.info("user:shadow don't match user's ID. cookie:{}|status:{} ", cookie, result.getStatus());
				return result;
			}

			String oldNick = jedis.hget("user:nick", userId);
			if (!nick.equals(oldNick)) {
				// 新旧Nick不一致时修改
				jedis.hset("user:nick", userId, nick);
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("", e);
			return result;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		logger.info("response: userId:{}|cookie:{}|nick:{}|status:{}", userId, cookie, nick, result.getStatus());
		return result;
	}

}
