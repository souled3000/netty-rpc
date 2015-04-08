package com.blackcrystalinfo.platform.util;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

/**
 * 
 * @author j
 * 
 */
@SuppressWarnings("restriction")
public class CookieUtil {
	private static final Logger logger = LoggerFactory.getLogger(CookieUtil.class);
	/**
	 * cookie过期时间
	 */
	public static final String EXPIRE_SEC = Constants.getProperty("expire", "300");

	/**
	 * 用户认证
	 */
	public static final String USER_SALT = Constants.getProperty("USER_SALT", "black_crystal");

	/**
	 * 设备cookie生成的盐
	 */
	public static final String DEVICE_SALT = Constants.getProperty("DEVICE_SALT", "BlackCrystalDevice14529");
	/**
	 * WEBSOCKET的盐
	 */
	public static final String WEBSOCKET_SALT = Constants.getProperty("WEBSOCKET_SALT", "BlackCrystalWb14527");

	/**
	 * 生成cookie
	 * 
	 * @param userId
	 *            用户Id
	 * @return cookie
	 * @throws NoSuchAlgorithmException
	 */
	public static String encode4user(String userId, String expire,String shadow) throws NoSuchAlgorithmException {
		BASE64Encoder encoder = new BASE64Encoder();
		String cookie = "";
		String sha1Value = encodeSha1(userId, expire);
		String beforeAes = String.format("%s|%s|%s", userId, expire, sha1Value);
		cookie = encoder.encode(beforeAes.getBytes());
		cookie = cookie.replace("+", "%2B");
		String up = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId+shadow).getBytes()));
		return cookie+"-"+up;
	}

	/**
	 * 解析用户Id
	 * 
	 * @param cookie
	 * @return 用户Id
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String[] decode(String cookie) throws NoSuchAlgorithmException, IOException{

		cookie = cookie.replace("%2B", "+");

		BASE64Decoder decoder = new BASE64Decoder();

		byte[] cookieStrByte = decoder.decodeBuffer(cookie);

		String beforeAes = new String(cookieStrByte);

		String[] parts = beforeAes.split("\\|");

		if(parts.length<3){
			throw new ArrayIndexOutOfBoundsException("parts's length less than three.");
		}
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

	public static String gotUserIdFromCookie(String cookie) throws Exception{
		String[] cs = cookie.split("-");
		String[] cookies = CookieUtil.decode(cs[0]);
		String userId = cookies[0];
		return userId;
	}

	
	
	public static String generateKey(String id, String timestamp, String expire) throws NoSuchAlgorithmException {
		String key = "";
		String buf = String.format("%s|%s|%s", id, timestamp, expire);

		String md5_buf = String.format("%s|%s|%s|%s", id, timestamp, expire, WEBSOCKET_SALT);
		byte[] bytes = MessageDigest.getInstance("MD5").digest(md5_buf.getBytes());

		BASE64Encoder encoder = new BASE64Encoder();
		String md5Str = encoder.encode(bytes);

		key = String.format("%s|%s", buf, md5Str);
		return key;
	}
	
	public static String generateDeviceKey(String mac, String id) throws NoSuchAlgorithmException, InvalidKeyException {
		String cookie = "";

		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKey secret = new SecretKeySpec(DEVICE_SALT.getBytes(), "HMACSHA256");
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


	public static boolean verifyDeviceKey(String mac, String cookie,String id) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		boolean result = false;

//		String id = extractDeviceId(cookie);
		if (null != cookie) {
			cookie = cookie.replace("+", "%2B");
			if (cookie.equals(generateDeviceKey(mac, id))) {
				result = true;
			}
		}

		return result;
	}

	private static String encodeSha1(String id, String expire) throws NoSuchAlgorithmException {
		String result = "";

		String beforeSha1 = String.format("%s|%s|%s", id, expire, USER_SALT);
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

	
	public static boolean validateMobileCookie(String cookie,String shadow){
		String[] cs = cookie.split("-");
		if (cs.length != 2) {
			return false;
		}
		String csmd5 = cs[2];
		try {
			String[] cookies = CookieUtil.decode(cs[0]);
			String userId = cookies[0];
			String csmd52 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId + shadow).getBytes()));
			if (!csmd5.equals(csmd52)) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static void main(String[] args)throws Exception {
//		String s = "gD BOoD";
//		String x = generateDeviceKey("mac1110","-1092");
//		System.out.println(x);
//		x =generateKey("mac1110", "123241412", "40");
//		System.out.println(x);
		
		/*************************用户cookie测试开始**********************/
		String cookie="NjR8MzAwfGY0NGE3OTg0OGI1NTY5NDExYWNjZTY1YzM1MjUzYTQwZTJhMjQ3";
		String shadow="1000:5b42403361333030323434:b3c02405995c1fcf4a6647b69da1a4df48e796181a8574db0aafae0206efc203880fd9a39ca32a4693709754ea23caf16061327ef7e4429dfe5024ed704616c7";
		String userId="64";
//		System.out.println(CookieUtil.validateMobileCookie(cookie, shadow));
		
//		cookie = CookieUtil.encode4user(userId, CookieUtil.EXPIRE_SEC,shadow);
		
//		System.out.println(cookie);
//		System.out.println(CookieUtil.validateMobileCookie(cookie, shadow));
		/*************************用户cookie测试结束**********************/
		
		
		/*************************设备cookie验证***********************/
		String mac = "mskqMMCjjNE=";
		cookie= "-1439|wbWGbJOIJd73kTwd7vG92puQVkwl1HTJ1li+onN6k3I=";
		String id = "-1439";
		boolean b = CookieUtil.verifyDeviceKey(mac, cookie,id);
		System.out.println("设备验证结果"+b);
		/*************************设备cookie验证结束***********************/
		String s = "/mobile/something";
		System.out.println(s.substring(0, s.lastIndexOf("/")));
		
		
		byte[] bs = new byte[6];
		bs[0]=68;
		bs[1]=68;
		bs[2]=68;
		bs[3]=0;
		bs[4]=0;
		bs[5]=0;
		String ss = new String(bs);
		logger.info("mac not matched cookie mac:{}|"+ss, ss);
		System.out.println(ss);
		
	}
}
