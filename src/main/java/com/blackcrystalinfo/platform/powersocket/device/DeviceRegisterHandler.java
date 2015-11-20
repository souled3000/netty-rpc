package com.blackcrystalinfo.platform.powersocket.device;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;
import com.blackcrystalinfo.platform.service.InternalException;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;
import com.guru.LicenseHelper;

import redis.clients.jedis.Jedis;

@Controller("/api/device/register")
public class DeviceRegisterHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DeviceRegisterHandler.class);

	@Autowired
	private IDeviceSrv deviceSrv;

	@Override
	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		String sn = req.getString("sn");
		String dv = req.getString("dv");
		String pid = req.getString("pid");
		String name = req.getString("name");
		String sign = req.getString("sign");
		String isUnbind = req.getString("isUnbind");
		return deal(mac, sn, dv, pid, name, sign, isUnbind);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter("mac");
		String sn = req.getParameter("sn");
		String dv = req.getParameter("dv");
		String pid = req.getParameter("pid");
		String name = req.getParameter("name");
		String sign = req.getParameter("sign");
		String isUnbind = req.getParameter("isUnbind");
		return deal(mac, sn, dv, pid, name, sign, isUnbind);
	}

	private Object deal(String... args) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put("status", -1);

		String mac = args[0];
		String sn = args[1];
		String dv = args[2];
		String pid = args[3];
		String name = args[4];
		String sign = args[5];
		String isUnbind = args[6];

		if (!isValidDev(mac, sign)) {
			r.put("status", 1);
			logger.info("Device regist failed, status:{}", r.get("status"));
			return r;
		}

		String cookie = "";
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			// 1. 设备MAC是否已被注册
			Long id = deviceSrv.getIdByMac(mac);
			if (null == id) {

				Long lPid = null;
				Integer iDv = null;
				if (StringUtils.isNotBlank(pid)) {
					lPid = Long.valueOf(pid);
				}
				if (StringUtils.isNotBlank(dv)) {
					iDv = Integer.valueOf(dv);
				}
				id = jedis.decr("dvpk");
				deviceSrv.regist(id ,mac, sn, name, lPid, iDv);
				id = deviceSrv.getIdByMac(mac);
			}

			if (null != id) {
				cookie = CookieUtil.generateDeviceKey(mac, id.toString());

				String licenseKey = parseLicenseKey(mac, sign);
				String licenseKeyCookie = licenseKey + cookie;
				String keyMd5 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest(licenseKeyCookie.getBytes()));
				
				jedis.hset("device:keymd5", id.toString(), keyMd5);
			}
			r.put("id", id);
			r.put("cookie", cookie);

			// 强制解绑
			if ("true".equalsIgnoreCase(isUnbind)) {
				String owner = jedis.hget("device:owner", id.toString());
				jedis.hdel("device:owner", id.toString());
				jedis.srem("u:" + owner + ":devices", id.toString());
			}

		} catch (Exception e) {
			logger.error("Device regist error mac:{}|sn:{}|dv:{}", mac, sn, dv, e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		r.put("status", 0);
		return r;
	}

	private String parseLicenseKey(String mac, String key) {
		String result = "";
		return result;
	}

	private boolean isValidDev(String mac, String key) {
		byte[] sign = ByteUtil.fromHex(key);

		boolean needValid = Constants.DEV_REG_VALID;
		if (!needValid) {
			// 不用校验，方便调试
			return true;
		}

		String lic_path = Constants.DEV_REG_LIC_PATH;
		int ret = LicenseHelper.validateLicense(mac.getBytes(), new ByteArrayInputStream(sign), lic_path);
		if (ret != 0) {
			logger.error("valid failed, ret = {}", ret);
			return false;
		}

		return true;
	}

}
