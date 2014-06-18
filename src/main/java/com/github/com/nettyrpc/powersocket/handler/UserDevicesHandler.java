package com.github.com.nettyrpc.powersocket.handler;

import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.user.UserDevicesResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

public class UserDevicesHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UserDevicesHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		UserDevicesResponse result = new UserDevicesResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");

		String[] infos = null;
		try {
			infos = CookieUtil.decode(cookie);

			String tmpUserId = infos[0];
			if (!userId.equals(tmpUserId)) {
				throw new IllegalArgumentException();
			}
		} catch (Exception e) {
			logger.error("Cookie decode error.");
			result.setStatusMsg("Cookie decode error.");
			return result;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			Set<String> devIds = jedis.smembers("bind:user:" + userId);

			result.setStatus(0);
			result.setBindedDevices(new ArrayList<String>(devIds));
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Get user's device error.");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}

}
