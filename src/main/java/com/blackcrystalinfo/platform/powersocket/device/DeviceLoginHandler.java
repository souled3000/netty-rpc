package com.blackcrystalinfo.platform.powersocket.device;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.NumberByte;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;
import com.blackcrystalinfo.platform.service.InternalException;
import com.blackcrystalinfo.platform.util.cryto.AESCoder;

import redis.clients.jedis.Jedis;

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

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 根据mac获取deviceId
			Long id = deviceSrv.getIdByMac(mac);
			if (null == id) {
				r.put("status", 2);
				return r;
			}

			// // 根据deviceId获取keyMd5(平台授权密钥)
			byte[] b = jedis.hget("sq".getBytes(), String.valueOf(id).getBytes());
			if (b != null && b.length != 16) {
				r.put("status", 3);
				return r;
			}
			// cookie = parseRealCookie(cookie, keyMd5);
			// r.put("keyMd5", keyMd5);
			// // 验证cookie
			try {
				cookie=Hex.encodeHexString(AESCoder.decryptNp(Hex.decodeHex(cookie.toCharArray()), b));
				if (!CookieUtil.isDvCki(Hex.decodeHex(mac.toCharArray()), cookie)) {
					r.put("status", 1);
					return r;
				}
			} catch (Exception e) {
				logger.error("", e);
				return r;
			}

			// 生成临时密钥
			UUID uuid = UUID.randomUUID();
			byte[] a = new byte[16];
			System.arraycopy(NumberByte.long2Byte(uuid.getMostSignificantBits()), 0, a, 0, 8);
			System.arraycopy(NumberByte.long2Byte(uuid.getLeastSignificantBits()), 0, a, 8, 8);
			// 用keyMd5加密临时密钥
			byte[] keyCipher = AESCoder.encryptNp(a, b);
			r.put("tmp", Hex.encodeHexString(keyCipher));
			// 算密证
			byte[] c = new byte[32];
			System.arraycopy(a, 0, c, 0, 16);
			System.arraycopy(b, 0, c, 16, 16);
			byte[] MiZheng = MessageDigest.getInstance("MD5").digest(c);
			// 存储密证
			jedis.set(("MZ:"+mac), Hex.encodeHexString(MiZheng));
			logger.info("\n\n临时密钥明文:{}\n临时密钥密文:{}\n平台授权码:{}\n密证:{}\n\n", Hex.encodeHexString(a), Hex.encodeHexString(keyCipher), Hex.encodeHexString(b), Hex.encodeHexString(MiZheng));
			// 根据deviceId获取设备属主
			String owner = jedis.hget("device:owner", id.toString());
			r.put("owner", owner);
			r.put("id", id);
			if (StringUtils.isNotBlank(pid)) {
				deviceSrv.setPidById(id, Long.valueOf(pid));
			}

		} catch (Exception e) {
			logger.error("", e);
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
	@SuppressWarnings("unused")
	private String parseRealCookie(String cookie, String keyMd5) {
		String result = null;

		// TODO: use keyMd5 decode the cookie
		result = cookie;

		return result;
	}
}
