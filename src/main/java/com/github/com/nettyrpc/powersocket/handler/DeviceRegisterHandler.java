package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.device.DeviceRegisterResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

public class DeviceRegisterHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UserLoginHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		DeviceRegisterResponse result = new DeviceRegisterResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String sn = HttpUtil.getPostValue(req.getParams(), "sn");
		String name = HttpUtil.getPostValue(req.getParams(), "deviceName");
		String regTime = String.valueOf(System.currentTimeMillis());

		if (!isValidSN(sn)) {
			result.setStatusMsg("SN is invalid!");
			return result;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 邮箱是否注册
			String existId = jedis.hget("device:mactoid", mac);
			if (null != existId) {
				result.setStatusMsg("Regist failed! mac exists.");
				return result;
			}

			// 2. 生成设备ID
			String deviceId = String.valueOf(jedis.incr("device:nextid"));

			// 3. 记录设备Id
			jedis.hset("device:mactoid", mac, deviceId);

			// 4. 记录MAC地址
			jedis.hset("device:mac", deviceId, mac);

			// 5. 记录设备SN号
			jedis.hset("device:sn", deviceId, sn);

			// 6. 设备注册时间
			jedis.hset("device:regtime", deviceId, regTime);

			// 7. 设备名称
			jedis.hset("device:name", deviceId, name);

			String cookie = CookieUtil.encode(mac, deviceId);
			String timeStamp = String.valueOf(System.currentTimeMillis());
			String proxyKey = CookieUtil.generateKey(deviceId, timeStamp, mac);
			String proxyAddr = CookieUtil.getWebsocketAddr();

			result.setStatus(0);
			result.setDeviceId(deviceId);
			result.setCookie(cookie);
			result.setProxyKey(proxyKey);
			result.setProxyAddr(proxyAddr);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Device regist error.");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;

	}

	private boolean isValidSN(String sn) {
		// TODO:
		return true;
	}

}
