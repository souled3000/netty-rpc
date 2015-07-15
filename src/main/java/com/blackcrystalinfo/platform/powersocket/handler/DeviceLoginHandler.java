package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.dao.IDeviceDao;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.CookieUtil;

@Controller("/api/device/login")
public class DeviceLoginHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(DeviceLoginHandler.class);

	@Autowired
	private IDeviceDao deviceDao;

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

		try {
			// 1. 根据mac获取deviceId
			Long id = deviceDao.getIdByMac(mac);
			if (null == id) {
				r.put("status", 2);
				logger.info("Mac does not exist. mac:{}|cookie:{}|status:{}",
						mac, cookie, r.get("status"));
				return r;
			}
			try {
				if (!CookieUtil.verifyDeviceKey(mac, cookie, id.toString())) {
					r.put("status", 1);
					logger.info(
							"mac not matched cookie mac:{}|cookie:{}|status:{}",
							mac, cookie, r.get("status"));
					return r;
				}
			} catch (Exception e) {
				logger.error("Cookie decode error. mac:{}|cookie:{}|status:{}",
						mac, cookie, r.get("status"));
				return r;
			}

			if (StringUtils.isNotBlank(pid)) {
				deviceDao.setPidById(id, Long.valueOf(pid));
			}

			String proxyKey = CookieUtil.generateKey(id.toString(),
					String.valueOf(System.currentTimeMillis() / 1000),
					CookieUtil.EXPIRE_SEC);
			String proxyAddr = CometScanner.take();

			logger.info("proxykey:{} | size:{} | proxyAddr:{} ", proxyKey,
					proxyKey.getBytes().length, proxyAddr);
			r.put("proxyKey", proxyKey);
			r.put("proxyAddr", proxyAddr);

		} catch (Exception e) {
			logger.error("Device login error  mac:{}|cookie:{}|status:{}|e:{}", mac,
					cookie, r.get("status"), e);
			return r;
		}

		logger.info("response: mac:{}|cookie:{}|status:{}", mac, cookie,
				r.get("status"));
		r.put("status", 0);
		return r;
	}
}
