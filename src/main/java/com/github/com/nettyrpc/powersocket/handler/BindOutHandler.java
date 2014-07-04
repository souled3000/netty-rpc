package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.bind.BindOutResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

public class BindOutHandler implements IHandler {
	private static final Logger logger = LoggerFactory
			.getLogger(BindOutHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		BindOutResponse result = new BindOutResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");

		String deviceId = "";
		try {
			if (!CookieUtil.verifyDeviceKey(mac, cookie)) {
				logger.error("mac={}&cookie={}, not matched!!!", mac, cookie);
				result.setStatusMsg("mac not matched cookie");
				return result;
			}
			deviceId = CookieUtil.extractDeviceId(cookie);
		} catch (Exception e) {
			logger.error("Cookie decode error.");
			result.setStatusMsg("Cookie decode error.");
			return result;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			if ((jedis.srem("bind:device:" + deviceId, userId) == 0)
					|| (jedis.srem("bind:user:" + userId, deviceId) == 0)) {
				result.setStatusMsg("User device not binded!");
				return result;
			}

			result.setStatus(0);
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Bind out error");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}
}
