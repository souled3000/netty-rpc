package com.blackcrystalinfo.platform.powersocket.device;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
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
import com.blackcrystalinfo.platform.util.cryto.AESCoder;
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
		String isUnbind = req.getString("doUnbind");
		return deal(mac, sn, dv, pid, name, sign, isUnbind);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		
		String mac = req.getParameter("mac");
		String sn = req.getParameter("sn");
		String dv = req.getParameter("dv");
		String pid = req.getParameter("pid");
		String name = req.getParameter("name");
		String sign = req.getParameter("sign");
		String isUnbind = req.getParameter("doUnbind");
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

		
		byte[] cookie = null;
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
				deviceSrv.regist(id, mac, sn, name, lPid, iDv);
				id = deviceSrv.getIdByMac(mac);
			}

			
			cookie = CookieUtil.genDvCookie(Hex.decodeHex(mac.toCharArray()));
			
			byte[] licenseKey = parseLicenseKey(Hex.decodeHex(sign.toCharArray()));
			
			byte[] licenseKeyCookie = new byte[cookie.length + licenseKey.length];
			
			System.arraycopy(cookie, 0, licenseKeyCookie, 0, 32);
			System.arraycopy(licenseKey, 0, licenseKeyCookie, 32, 16);
			
			byte[] keyMd5 = MessageDigest.getInstance("MD5").digest(licenseKeyCookie);
			
			jedis.hset("sq".getBytes(), String.valueOf(id).getBytes(), keyMd5);
			
			
			byte[] cookieCipher = AESCoder.encryptNp(cookie, licenseKey);
			
			r.put("id", id);
			r.put("cookie", Hex.encodeHexString(cookieCipher));

			logger.info("\n\nCOOKIE明文:{}\nCOOKIE密文:{}\n平台授权码:{}\n\n\n",Hex.encodeHexString(cookie),Hex.encodeHexString(cookieCipher),Hex.encodeHexString(keyMd5));

			// 强制解绑
			if ("y".equalsIgnoreCase(isUnbind)) {
				String owner = jedis.hget("device:owner", id.toString());
				jedis.hdel("device:owner", id.toString());
				jedis.srem("u:" + owner + ":devices", id.toString());
			}

			
		} catch (Throwable e) {
			
			logger.error("Device regist error mac:{}|sn:{}|dv:{}", mac, sn, dv, e);
			return r;
		} finally {
			
			DataHelper.returnJedis(jedis);
		}
		
		r.put("status", 0);
		return r;
	}

	private byte[] parseLicenseKey(byte[] license) {
		byte[] result = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
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

	public static void main(String[] args) throws Exception{
		String mac = "020027430c000000";
		byte[] cookie = CookieUtil.genDvCookie(Hex.decodeHex(mac.toCharArray()));
		System.out.println(cookie.length);
		System.out.println(Hex.encodeHexString(cookie));
		byte[] licenseKey = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };
		byte[] licenseKeyCookie = new byte[cookie.length + licenseKey.length];
		System.arraycopy(cookie, 0, licenseKeyCookie, 0, 32);
		System.arraycopy(licenseKey, 0, licenseKeyCookie, 32, 16);
		byte[] keyMd5 = MessageDigest.getInstance("MD5").digest(licenseKeyCookie);
		byte[] cookieCipher = AESCoder.encrypt(cookie, licenseKey);
		
		System.out.println(licenseKeyCookie.length);
		System.out.println(Hex.encodeHexString(licenseKeyCookie));
		System.out.println(Hex.encodeHexString(cookie));
		System.out.println(keyMd5.length);
		System.out.println(cookie.length);
		System.out.println(cookieCipher.length);
	}
}
