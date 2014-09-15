package com.blackcrystalinfo.platform.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.device.DeviceRegisterResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class DeviceRegisterHandler implements IHandler {

	private static final Logger logger = LoggerFactory.getLogger(UserLoginHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		DeviceRegisterResponse result = new DeviceRegisterResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String sn = HttpUtil.getPostValue(req.getParams(), "sn");
		String dv = HttpUtil.getPostValue(req.getParams(), "dv");
		
		// String name = HttpUtil.getPostValue(req.getParams(), "deviceName");
		String regTime = String.valueOf(System.currentTimeMillis());
		logger.info("DeviceRegisterHandler begin mac:{}|sn:{}|bv:{}",mac,sn,dv);
		if (!isValidSN(sn)) {
			result.setStatusMsg("SN is invalid!");
			return result;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 设备MAC是否已被注册
			String existId = jedis.hget("device:mactoid", mac);
			if (null != existId) {
				// result.setStatusMsg("Regist failed! mac exists.");

				String cookie = CookieUtil.generateDeviceKey(mac, existId);
				// String timeStamp =
				// String.valueOf(System.currentTimeMillis());
				// String proxyKey = CookieUtil.generateKey(existId, timeStamp,
				// mac);
				// String proxyAddr = CookieUtil.getWebsocketAddr();

				result.setStatus(0);
				// result.setDeviceId(existId);
				result.setCookie(cookie);
			} else {
				// 2. 生成设备ID
				String deviceId = String.valueOf(jedis.decr("device:nextid"));

				Transaction tx = jedis.multi();

				// 3. 记录设备Id
				tx.hset("device:mactoid", mac, deviceId);

				// 4. 记录MAC地址
				tx.hset("device:mac", deviceId, mac);

				// 5. 记录设备SN号
				tx.hset("device:sn", deviceId, sn);
				

				// 6. 设备注册时间
				tx.hset("device:regtime", deviceId, regTime);

				// 7. 设备名称
				// tx.hset("device:name", deviceId, name);

				// 8. 设备类型
				tx.hset("device:dv", deviceId, dv);
				
				String cookie = CookieUtil.generateDeviceKey(mac, deviceId);
				// String timeStamp =
				// String.valueOf(System.currentTimeMillis());
				// String proxyKey = CookieUtil.generateKey(deviceId, timeStamp,
				// mac);
				// String proxyAddr = CookieUtil.getWebsocketAddr();

				result.setStatus(0);
				// result.setDeviceId(deviceId);
				result.setCookie(cookie);

				tx.exec();
			}

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Device regist error mac:{}|sn:{}|bv:{}",mac,sn,dv,e);
			return result;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response:  mac:{}|sn:{}|bv:{}",mac,sn,dv);
		return result;

	}

	private boolean isValidSN(String sn) {
		// TODO:
		return true;
	}

}
