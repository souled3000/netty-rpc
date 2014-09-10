package com.blackcrystalinfo.platform.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserChangeNickResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

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
		UserChangeNickResponse result = new UserChangeNickResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String nick = HttpUtil.getPostValue(req.getParams(), "nick");
		logger.info("UserChangeNickHandler begin userId:{}|cookie:{}|nick:{}",userId,cookie,nick);

		// 1. 校验cookie信息
		String[] infos = null;
		try {
			infos = CookieUtil.decode(cookie);

			if (!userId.equals(infos[0])) {
				result.setStatus(1);
				return result;
			}
		} catch (Exception e) {
			logger.error("",e);
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
			logger.error("",e);
			return result;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		logger.info("response: {}", result.getStatus());
		return result;
	}

}
