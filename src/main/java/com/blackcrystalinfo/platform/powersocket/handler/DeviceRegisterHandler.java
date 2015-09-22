package com.blackcrystalinfo.platform.powersocket.handler;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.dao.IDeviceDao;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;
import com.guru.LicenseHelper;

@Controller("/api/device/register")
public class DeviceRegisterHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DeviceRegisterHandler.class);

	@Autowired
	private IDeviceDao deviceDao;

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

		logger.info("Device regist begin mac:{}|sn:{}|dv:{}|sign:{}|isUnbind:{}", mac, sn, dv, sign, isUnbind);
		if (!isValidDev(mac, sign)) {
			r.put("status", 1);
			logger.info("Device regist failed, status:{}", r.get("status"));
			return r;
		}

		Jedis jedis = null;
		try {
			// 1. 设备MAC是否已被注册
			Long existId = deviceDao.getIdByMac(mac);
			if (null == existId) {

				Long lPid = null;
				Integer iDv = null;
				if (StringUtils.isNotBlank(pid)) {
					lPid = Long.valueOf(pid);
				}
				if (StringUtils.isNotBlank(dv)) {
					iDv = Integer.valueOf(dv);
				}

				deviceDao.regist(mac, sn, name, lPid, iDv);
				existId = deviceDao.getIdByMac(mac);
			}

			String cookie = "";
			if (null != existId) {
				cookie = CookieUtil.generateDeviceKey(mac, existId.toString());

				String licenseKey = parseLicenseKey(mac, sign);
				String licenseKeyCookie = licenseKey + cookie;
				String keyMd5 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest(licenseKeyCookie.getBytes()));
				jedis = DataHelper.getJedis();
				jedis.hset("device:keymd5", existId.toString(), keyMd5);
			}
			r.put("cookie", cookie);

			// 强制解绑
			if ("true".equalsIgnoreCase(isUnbind)) {
				String owner = jedis.hget("device:owner", existId.toString());
				jedis.hdel("device:owner", existId.toString());
				jedis.srem("u:" + owner + ":devices", existId.toString());
			}

		} catch (Exception e) {
			logger.error("Device regist error mac:{}|sn:{}|dv:{}", mac, sn, dv, e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response:  mac:{}|sn:{}|dv:{}", mac, sn, dv);
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
