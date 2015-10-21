package com.blackcrystalinfo.platform.powersocket.device;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.server.CometScanner;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;
import com.blackcrystalinfo.platform.service.InternalException;

@Controller("/api/device/login")
public class DeviceLoginHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DeviceLoginHandler.class);

	@Autowired
	private IDeviceSrv deviceSrv;

	@Override
	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		String pid = req.getString("pid");
		String cookie = req.getString("cookie");
		return deal(mac, pid, cookie);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter("mac");
		String pid = req.getParameter("pid");
		String cookie = req.getParameter("cookie");
		return deal(mac, pid, cookie);
	}

	private Object deal(String... args) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put("status", -1);

		String mac = args[0];
		String pid = args[1];
		String cookie = args[2];

		logger.info("DeviceLoginHandler begin mac:{}|cookie:{}", mac, cookie);

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 根据mac获取deviceId
			Long id = deviceSrv.getIdByMac(mac);
			if (null == id) {
				r.put("status", 2);
				logger.info("Mac does not exist. mac:{}|cookie:{}|status:{}", mac, cookie, r.get("status"));
				return r;
			}

			// 根据deviceId获取keyMd5
			String keyMd5 = jedis.hget("device:keymd5", id.toString());
			cookie = parseRealCookie(cookie, keyMd5);
			r.put("keyMd5", keyMd5);

			// 验证cookie
			try {
				if (!CookieUtil.verifyDeviceKey(mac, cookie, id.toString())) {
					r.put("status", 1);
					logger.info("mac not matched cookie mac:{}|cookie:{}|status:{}", mac, cookie, r.get("status"));
					return r;
				}
			} catch (Exception e) {
				logger.error("Cookie decode error. mac:{}|cookie:{}|status:{}", mac, cookie, r.get("status"));
				return r;
			}

			// 根据deviceId获取设备属主
			String owner = jedis.hget("device:owner", id.toString());
			r.put("owner", owner);

			if (StringUtils.isNotBlank(pid)) {
				deviceSrv.setPidById(id, Long.valueOf(pid));
			}

			String proxyKey = CookieUtil.generateKey(id.toString(), String.valueOf(System.currentTimeMillis() / 1000), CookieUtil.EXPIRE_SEC);
			String proxyAddr = CometScanner.take();

			logger.info("proxykey:{} | size:{} | proxyAddr:{} ", proxyKey, proxyKey.getBytes().length, proxyAddr);
			r.put("proxyKey", proxyKey);
			r.put("proxyAddr", proxyAddr);

		} catch (Exception e) {
			logger.error("Device login error  mac:{}|cookie:{}|status:{}|e:{}", mac, cookie, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		r.put("status", 0);
		return r;
	}

	/**
	 * AES128 Decode
	 * 
	 * @param cookie
	 * @param keyMd5
	 * @return
	 */
	private String parseRealCookie(String cookie, String keyMd5) {
		String result = null;

		// TODO: use keyMd5 decode the cookie
		result = cookie;

		return result;
	}
}
