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
import com.blackcrystalinfo.platform.common.JedisHelper;
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
		byte[] licenseKey =  new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		if (Constants.DEV_REG_VALID) {
			Map<?,?> lm = LicenseHelper.validateLicenseMap(new ByteArrayInputStream(ByteUtil.fromHex(sign)), Constants.DEV_REG_LIC_PATH);
			if (StringUtils.isNotBlank((String)lm.get("ErrorCode"))) {
				r.put("status", 1);
				logger.info("Device regist failed, status:{};KeyPATH:{};ErrorCode:{};ErrorMsg:{}", r.get("status"),Constants.DEV_REG_LIC_PATH,lm.get("ErrorCode"),lm.get("ErrorMsg"));
				return r;
			}else{
				if(!mac.equals((String)lm.get("mac"))){
					r.put("status", 1);
					logger.info("Device regist failed, status:{};mac:{};mtime:{};sn:{};token:{}", r.get("status"),lm.get("mac"),lm.get("mtime"),lm.get("sn"),lm.get("token"));
					return r;
				}
				licenseKey = ByteUtil.fromHex((String)lm.get("token"));
			}
		}

		
		byte[] cookie = null;
		Jedis jedis = null;
		
		try {
			jedis = JedisHelper.getJedis();
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
				
				deviceSrv.regist(mac, sn, name, lPid, iDv);
				id = deviceSrv.getIdByMac(mac);
			}

			
			cookie = CookieUtil.genDvCki(Hex.decodeHex(mac.toCharArray()));
			
			
			
			byte[] licenseKeyCookie = new byte[cookie.length + licenseKey.length];
			
			System.arraycopy(cookie, 0, licenseKeyCookie, 0, 32);
			System.arraycopy(licenseKey, 0, licenseKeyCookie, 32, 16);
			
			byte[] keyMd5 = MessageDigest.getInstance("MD5").digest(licenseKeyCookie);
			
			jedis.hset("sq".getBytes(), String.valueOf(id).getBytes(), keyMd5);
			
			
			byte[] cookieCipher = AESCoder.encryptNp(cookie, licenseKey);
			
			r.put("id", id);
			r.put("cookie", Hex.encodeHexString(cookieCipher));

			logger.info("\nLicenseKey:{}\n\nCOOKIE明文:{}\nCOOKIE密文:{}\n平台授权码:{}\n\n\n",Hex.encodeHexString(licenseKey),Hex.encodeHexString(cookie),Hex.encodeHexString(cookieCipher),Hex.encodeHexString(keyMd5));

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
			
			JedisHelper.returnJedis(jedis);
		}
		
		r.put("status", 0);
		return r;
	}

	public static void main(String[] args) throws Exception{
		f1();
	}
	
	public static void f() throws Exception{
		String mac = "0000b0d59d63f58a";
		String sign = "0003730ba305df8c014be8863ef3152760ab11d5a4f8204e5d485805531792354eb0cb6e38e7b5f97733089080dc46d20c117562d36ca49add3ee9fa6370f5177373c2b1722941df2bfe174d45a67634d884e0ae4f6124066951a469ca9f09053feb47624e64c02ebb030b098a5daa595db40b1865cd647697884c513b0feaa68f38";
//		String mac = "0000b0d59d63f81d";
//		String sign = "000349dc7b93dc47502e4bb6aa622f2a0a7eeea018a8f9be46662cd894c7380da0205e3e24fa2e4976788721069492ca59d11013f32a44149d954a986f932193652037f0be4494554abf8ef9aaaea2650dc75d75ae08a3e13492566e4566216c009434a19ac06acd52338a522a17599701e99e59f204d89138370a3b308044fe3fb2";
		Map<?,?> lm = LicenseHelper.validateLicenseMap(new ByteArrayInputStream(ByteUtil.fromHex(sign)), "d:\\lib\\");
		byte[] licenseKey = ByteUtil.fromHex((String)lm.get("token"));
		byte[] cookie = CookieUtil.genDvCki(Hex.decodeHex(mac.toCharArray()));
		System.out.println("licenseKey:\t"+Hex.encodeHexString(licenseKey));
		System.out.println("cookie:\t"+Hex.encodeHexString(cookie));
		byte[] cookieCipher = AESCoder.encryptNp(cookie, licenseKey);
		System.out.println("cookieCipher:\t"+Hex.encodeHexString(cookieCipher));
		if (StringUtils.isNotBlank((String)lm.get("ErrorCode"))) {
			logger.info("Device regist failed, KeyPATH:{};ErrorCode:{};ErrorMsg:{}",Constants.DEV_REG_LIC_PATH,lm.get("ErrorCode"),lm.get("ErrorMsg"));
		}else{
			if(!mac.equals((String)lm.get("mac"))){
				logger.info("Device regist failed, mac:{};mtime:{};sn:{};token:{}", lm.get("mac"),lm.get("mtime"),lm.get("sn"),lm.get("token"));
			}
			licenseKey = ByteUtil.fromHex((String)lm.get("token"));
		}
		
		
		int ret = LicenseHelper.validateLicense(mac.getBytes(), new ByteArrayInputStream(ByteUtil.fromHex(sign)), "d:\\lib\\");
	    if (ret != 0) {
	      logger.error("valid failed, ret = {}", Integer.valueOf(ret));
	    }
	}
	public static void f1() throws Exception{
		String mac = "0000b0d59d63f5dd";
		byte[] cookie = CookieUtil.genDvCki(Hex.decodeHex(mac.toCharArray()));
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
		System.out.println(Integer.valueOf("1828782337"));
		
		
	}
}
