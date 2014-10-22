package com.blackcrystalinfo.platform.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import sun.misc.BASE64Decoder;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.device.DeviceRegisterResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class DeviceRegisterHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserLoginHandler.class);

	@Override
	public Object rpc(JSONObject req) throws InternalException {
		BASE64Decoder decoder = new BASE64Decoder();
		DeviceRegisterResponse result = new DeviceRegisterResponse();
		result.setStatus(-1);
//		result.setStatusMsg("");
//		result.setUrlOrigin(req.getUrlOrigin());

		String mac = req.getString("mac");
		String sn = req.getString("sn");
		String dv = req.getString("dv");
		
		// String name = req.getString("deviceName");
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

				result.setStatus(1);
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
				tx.hset("device:mac2", deviceId, ByteUtil.toHex(decoder.decodeBuffer(mac.replace(' ', '+'))));

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

	public static void main(String[] args) throws Exception{
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] bs = decoder.decodeBuffer("Dgw1DAD6");
		System.out.println(new String(bs,"utf8"));
		System.out.println(ByteUtil.toHex(bs));
	}
}
