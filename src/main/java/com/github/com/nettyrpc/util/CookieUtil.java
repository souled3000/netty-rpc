package com.github.com.nettyrpc.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	public static final String WEBSOCKET_ADDR = "ws://127.0.0.1/ws";

	/**
	 * 
	 */
	public static final String PRIVIATE_KEY = "black_crystal";

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
	public static String generateKey(String id, String timestamp, String expire)
			throws NoSuchAlgorithmException {
		String key = "";

		BASE64Encoder encoder = new BASE64Encoder();

		String timestampB = encoder.encode(timestamp.getBytes());

		String kWbSalt = "BlackCrystalWb14527";

		String buf = String.format("0|%s|%s|%s", id, timestampB, expire);
		String md5_buf = String.format("0|%s|%s|%s|%s", id, timestamp, expire,
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
