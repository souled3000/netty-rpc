package com.github.com.nettyrpc.util;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * @author j
 * 
 */
@SuppressWarnings("restriction")
public class CookieUtil {

	/**
	 * cookie过期时间
	 */
	public static final String EXPIRE_SEC = "300";

	/**
	 * websocket地址
	 */
	public static final String WEBSOCKET_ADDR = Constants.WEBSOCKET_ADDR;

	/**
	 * 
	 */
	public static final String PRIVIATE_KEY = "black_crystal";

	/**
	 * 设备cookie生成的盐
	 */
	public static final String DEVICESALT_KEY = "BlackCrystalDevice14529";

	/**
	 * 生成cookie
	 * 
	 * @param userId
	 *            用户Id
	 * @return cookie
	 * @throws NoSuchAlgorithmException
	 */
	public static String encode(String userId, String expire)
			throws NoSuchAlgorithmException {
		String cookie = "";

		String sha1Value = encodeSha1(userId, expire);

		String beforeAes = String.format("%s|%s|%s", userId, expire, sha1Value);

		BASE64Encoder encoder = new BASE64Encoder();

		cookie = encoder.encode(beforeAes.getBytes());

		cookie = cookie.replace("+", "%2B");

		return cookie;
	}

	/**
	 * 解析用户Id
	 * 
	 * @param cookie
	 *            cookie
	 * @return 用户Id
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String[] decode(String cookie)
			throws NoSuchAlgorithmException, IOException {

		cookie = cookie.replace("%2B", "+");

		BASE64Decoder decoder = new BASE64Decoder();

		byte[] cookieStrByte = decoder.decodeBuffer(cookie);

		String beforeAes = new String(cookieStrByte);

		String[] parts = beforeAes.split("\\|");

		String userId = "";
		String expire = "";
		String sha1Value = "";

		userId = parts[0];
		expire = parts[1];
		sha1Value = parts[2];

		if (!sha1Value.equals(encodeSha1(userId, expire))) {
			return null;
		}

		return new String[] { userId, expire };
	}

	public static String generateDeviceKey(String mac, String id)
			throws NoSuchAlgorithmException, InvalidKeyException {
		String cookie = "";

		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKey secret = new SecretKeySpec(DEVICESALT_KEY.getBytes(),
				"HMACSHA256");
		hmac.init(secret);

		byte[] doFinal = hmac.doFinal(mac.getBytes());

		BASE64Encoder encoder = new BASE64Encoder();
		String macEncode = encoder.encode(doFinal);

		cookie = String.format("%s|%s", id, macEncode);

		cookie = cookie.replace("+", "%2B");

		return cookie;
	}

	public static String extractDeviceId(String cookie) throws IOException {
		cookie = cookie.replace("%2B", "+");

		String[] parts = cookie.split("\\|");

		if (null == parts || parts.length < 2) {
			return null;
		}

		String deviceId = "";
		deviceId = parts[0];

		return deviceId;
	}

	/**
	 * 注册成功后，返回的用于websocket连接的key
	 * 
	 * @param id
	 *            用户Id/设备Id
	 * @param timestamp
	 *            系统时间戳
	 * @return websocket连接的key
	 * @throws NoSuchAlgorithmException
	 */
	public static String generateKey(String id, String mac, String alias, String expire, String[] bindedIds, String timestamp)
			throws NoSuchAlgorithmException {
		String key = "";

		StringBuilder sb = new StringBuilder();
		for (String bindedId : bindedIds)
		{
			sb.append(bindedId).append(",");
		}
		String bindedIdStr = sb.toString();
		if (bindedIdStr.endsWith(","))
		{
			bindedIdStr = bindedIdStr.substring(0, bindedIdStr.length()-1);
		}
		String buf = String.format("0|%s|%s|%s|%s|%s", id, mac, alias, expire, bindedIdStr);
		
		BASE64Encoder encoder = new BASE64Encoder();
		String timestampB = encoder.encode(timestamp.getBytes());
		String kWbSalt = "BlackCrystalWb14527";
		String md5_buf = String.format("0|%s|%s|%s|%s", id, timestampB, expire,
				kWbSalt);
		byte[] bytes = MessageDigest.getInstance("MD5").digest(
				md5_buf.getBytes());
		String md5Str = encoder.encode(bytes);

		key = String.format("%s|%s", buf, md5Str);
		return key;
	}

	/**
	 * 用于连接websocket的地址
	 * 
	 * @return websocket的地址
	 */
	public static String getWebsocketAddr() {
		return WEBSOCKET_ADDR;
	}

	public static boolean verifyDeviceKey(String mac, String cookie)
			throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		boolean result = false;

		String id = extractDeviceId(cookie);
		if (null != cookie) {
			cookie = cookie.replace("+", "%2B");
			if (cookie.equals(generateDeviceKey(mac, id))) {
				result = true;
			}
		}

		return result;
	}

	private static String encodeSha1(String id, String expire)
			throws NoSuchAlgorithmException {
		String result = "";

		String beforeSha1 = String.format("%s|%s|%s", id, expire, PRIVIATE_KEY);
		result = sha1(beforeSha1);

		return result;
	}

	private static String sha1(String str) throws NoSuchAlgorithmException {
		String result = "";

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(str.getBytes());
		result = bytetoString(digest);

		return result;
	}

	private static String bytetoString(byte[] digest) {
		String str = "";
		String tempStr = "";

		for (int i = 1; i < digest.length; i++) {
			tempStr = (Integer.toHexString(digest[i] & 0xff));
			if (tempStr.length() == 1) {
				str = str + "0" + tempStr;
			} else {
				str = str + tempStr;
			}
		}
		return str.toLowerCase();
	}

}
