package com.blackcrystalinfo.platform.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.device.DeviceLoginResponse;
import com.blackcrystalinfo.platform.util.CometScannerV2;
import com.blackcrystalinfo.platform.util.CookieUtil;

public class DeviceLoginHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DeviceLoginHandler.class);

	@Override
	public Object rpc(JSONObject req) throws InternalException {
		logger.info("request: {}", req);

		DeviceLoginResponse result = new DeviceLoginResponse();
		result.setStatus(-1);
//		result.setStatusMsg("");
//		result.setUrlOrigin(req.getUrlOrigin());

		String mac = req.getString("mac");
		String pid = req.getString("pid");
		String cookie = req.getString("cookie");

		logger.info("DeviceLoginHandler begin mac:{}|cookie:{}", mac, cookie);
		

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 根据mac获取deviceId
			String id = jedis.hget("device:mactoid", mac);
			if (null == id) {
				result.setStatus(2);
				logger.info("Mac does not exist. mac:{}|cookie:{}|status:{}", mac, cookie,result.getStatus());
				return result;
			}
			try {
				if (!CookieUtil.verifyDeviceKey(mac, cookie,id)) {
					result.setStatus(1);
					logger.info("mac not matched cookie mac:{}|cookie:{}|status:{}", mac, cookie,result.getStatus());
					return result;
				}
			} catch (Exception e) {
				logger.error("Cookie decode error. mac:{}|cookie:{}|status:{}", mac, cookie,result.getStatus(),3);
				return result;
			}
			// Set<String> users = jedis.smembers("bind:device:" + deviceId);
			// result.setBindedUsers(new ArrayList<String>(users));
			if(StringUtils.isNotBlank(pid))
			jedis.hset("device:pid", id, pid);
			
			String proxyKey = CookieUtil.generateKey(id, String.valueOf(System.currentTimeMillis() / 1000), CookieUtil.EXPIRE_SEC);
//			String proxyKey = CookieUtil.generateDeviceKey(mac,id);
			String proxyAddr = CometScannerV2.take();

			result.setStatus(0);
			logger.info("proxykey:{} | size:{} | proxyAddr:{} ", proxyKey, proxyKey.getBytes().length, proxyAddr);
			result.setProxyKey(proxyKey);
			result.setProxyAddr(proxyAddr);

		} catch (Exception e) {
			result.setStatus(-1);
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Device login error  mac:{}|cookie:{}|status:{}", mac, cookie,result.getStatus(),e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: mac:{}|cookie:{}|status:{}", mac, cookie,result.getStatus());
		return result;
	}
}
