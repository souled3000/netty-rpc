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
import com.github.com.nettyrpc.powersocket.dao.pojo.device.DeviceLoginResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

public class DeviceLoginHandler implements IHandler {
	private static final Logger logger = LoggerFactory
			.getLogger(DeviceLoginHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		DeviceLoginResponse result = new DeviceLoginResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");

		String[] infos = null;
		try {
			infos = CookieUtil.decode(cookie);
		} catch (Exception e) {
			logger.error("Cookie decode error.");
			result.setStatusMsg("Cookie decode error.");
			return result;
		}

		if (null == infos || infos.length != 2 || !mac.equals(infos[0])) {
			result.setStatusMsg("Cookie not matched.");
			return result;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 根据Email获取userId
			String deviceId = jedis.hget("device:mactoid", mac);
			if (null == deviceId) {
				result.setStatusMsg("Mac not exists, please regist it.");
				return result;
			}

			Set<String> users = jedis.smembers("bind:device:" + deviceId);

			String proxyKey = CookieUtil.generateKey(deviceId,
					String.valueOf(System.currentTimeMillis()),
					CookieUtil.EXPIRE_SEC);
			String proxyAddr = CookieUtil.getWebsocketAddr();

			result.setStatus(0);
			result.setBindedUsers(new ArrayList<String>(users));
			result.setProxyKey(proxyKey);
			result.setProxyAddr(proxyAddr);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Device login error");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}

}
