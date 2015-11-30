package com.blackcrystalinfo.platform.common;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.lang.StringUtils;

import com.blackcrystalinfo.platform.util.cryto.AESCoder;

/**
 * 
 * @author j
 * 
 */
public class CookieUtil {
//	private static final Logger logger = LoggerFactory.getLogger(CookieUtil.class);
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
//	public static String encode4user(String userId, String expire, String shadow) throws NoSuchAlgorithmException {
//		String cookie = "";
//		String timestamp = new String(System.currentTimeMillis() + "");
//		String sha1Value = encodeSha1(userId, expire, timestamp);
//		String beforeAes = String.format("%s|%s|%s|%s", userId, expire, timestamp, sha1Value);
//		cookie = Base64.encodeBase64String(beforeAes.getBytes());
//		cookie = cookie.replace("+", "%2B");
//		cookie = cookie.replaceAll("\r", "");
//		cookie = cookie.replaceAll("\n", "");
//		String up = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId + shadow).getBytes()));
//		return cookie + "-" + up;
//	}

	/**
	 * 解析用户Id
	 * 
	 * @param cookie
	 * @return 用户Id
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
//	private static String[] decodeUserCookie(String cookie) throws NoSuchAlgorithmException, IOException {
//
//		cookie = cookie.replace("%2B", "+");
//
//		byte[] cookieStrByte = Base64.decodeBase64(cookie);
//
//		String beforeAes = new String(cookieStrByte);
//
//		String[] parts = beforeAes.split("\\|");
//
//		if (parts.length < 4) {
//			throw new ArrayIndexOutOfBoundsException("parts's length less than four.");
//		}
//		String userId = "";
//		String expire = "";
//		String timestamp = "";
//		String sha1Value = "";
//
//		userId = parts[0];
//		expire = parts[1];
//		timestamp = parts[2];
//		sha1Value = parts[3];
//
//		if (!sha1Value.equals(encodeSha1(userId, expire, timestamp))) {
//			return null;
//		}
//
//		return new String[] { userId, expire, timestamp };
//	}

//	public static String gotUserIdFromCookie(String cookie) throws Exception {
//		String[] cs = cookie.split("-");
//		String[] cookies = CookieUtil.decodeUserCookie(cs[0]);
//		String userId = cookies[0];
//		return userId;
//	}

	public static String genWsKey(String id, String timestamp, String expire) throws NoSuchAlgorithmException {
		String key = "";
		String buf = String.format("%s|%s|%s", id, timestamp, expire);

		String md5_buf = String.format("%s|%s|%s|%s", id, timestamp, expire, WEBSOCKET_SALT);
		byte[] bytes = MessageDigest.getInstance("MD5").digest(md5_buf.getBytes());

		String md5Str = Base64.encodeBase64String(bytes);

		key = String.format("%s|%s", buf, md5Str);
		return key;
	}

	public static byte[] genDvCookie(byte[] mac) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKey secret = new SecretKeySpec(DEVICE_SALT.getBytes(), "HMACSHA256");
		hmac.init(secret);
		byte[] doFinal = hmac.doFinal(mac);
		return doFinal;
	}

	public static boolean isDvCookie(byte[] mac, String cookie) throws Exception {
		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKey secret = new SecretKeySpec(DEVICE_SALT.getBytes(), "HMACSHA256");
		hmac.init(secret);
		byte[] doFinal = hmac.doFinal(mac);
		if (StringUtils.equals(cookie, Hex.encodeHexString(doFinal))) {
			return true;
		}
		return false;
	}

//	private static String encodeSha1(String id, String expire, String timestamp) throws NoSuchAlgorithmException {
//		String result = "";
//
//		String beforeSha1 = String.format("%s|%s|%s|%s", id, expire, timestamp, USER_SALT);
//		result = sha1(beforeSha1);
//
//		return result;
//	}

//	private static String sha1(String str) throws NoSuchAlgorithmException {
//		String result = "";
//
//		MessageDigest md = MessageDigest.getInstance("SHA-1");
//		byte[] digest = md.digest(str.getBytes());
//		result = bytetoString(digest);
//
//		return result;
//	}

//	private static String bytetoString(byte[] digest) {
//		String str = "";
//		String tempStr = "";
//
//		for (int i = 1; i < digest.length; i++) {
//			tempStr = (Integer.toHexString(digest[i] & 0xff));
//			if (tempStr.length() == 1) {
//				str = str + "0" + tempStr;
//			} else {
//				str = str + tempStr;
//			}
//		}
//		return str.toLowerCase();
//	}
//
//	public static boolean validateMobileCookie(String cookie, String shadow) {
//		try {
//			String[] cs = cookie.split("-");
//			if (cs.length != 2) {
//				return false;
//			}
//			String csmd5 = cs[1];
//			String[] cookies = CookieUtil.decodeUserCookie(cs[0]);
//			String userId = cookies[0];
//			String csmd52 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId + shadow).getBytes()));
//			if (!csmd5.equals(csmd52)) {
//				return false;
//			}
//		} catch (Exception e) {
//			logger.error("", e);
//			return false;
//		}
//		return true;
//	}

	public static byte[] genCtlKey(String devId) {
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
			byte[] salt = new byte[16];
			sr.nextBytes(salt);
			return salt;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		// System.out.println(CookieUtil.gotUserIdFromCookie("NDh8MzAwfDM3YjY1NThmMzgwNWExZWMyYzQzMTI2N2M1ZGNiZWM0NDZlOWEx-35DF4E21C58D8038E7DE9A1C83DFFBBB"));
		System.out.println(Hex.encodeHexString(new byte[] { 0x01, 0x02, 0x0e }));
		System.out.println(Hex.encodeHexString(Hex.decodeHex("01020e".toCharArray())));
		byte[] b = genCtlKey("-29");
		System.out.println(Hex.encodeHexString(b));
		byte[] ctn = new byte[8];
		EndianUtils.writeSwappedLong(ctn, 0, -1L);
		System.out.println(Hex.encodeHexString(ctn));

//		String s = CookieUtil.gotUserIdFromCookie("OXwzMDB8MTQ0ODAwNjMyNjA5N3xlNjhjZTcxZjcwODU5YmRmZmYzMDIzOGFjYjA1ODcxMWYzZWQ4Yg==-D49ABECA71C325905426D5795257E500");
//		System.out.println(s);

		System.out.println("---------------------DEV-COOKIE----------------------------");
		byte[] mac = Hex.decodeHex("040027430c000000".toCharArray());
		System.out.println(Hex.encodeHexString(mac));
		byte[] dvcookie = CookieUtil.genDvCookie(mac);
		System.out.println(dvcookie.length);

		System.out.println("---------------------USR-COOKIE----------------------------");
		String usrCki = genUsrCki("100","1000:771585542c3b3f5e3d7ba67ec60f2ca790ddcb82881d022ab4ce60e684321969:2997ed9c153fc8d406011fbab5e73c97017c32289c5e714b3953bffb1b822000a4e01932dcc3071fde2cc71c9b763663f915a5b2dcf401569fe9af2ba6b3bf57");
		System.out.println(usrCki);

//		Object[] fids = gotUsr(URLDecoder.decode(usrCki,"utf8"));
		Object[] fids = gotUsr("pB8hTdq4TiNcRFZRg9qzr+9TJ4oxilknmG7+KZJPoiM=");
		System.out.println((String) fids[0]);
		System.out.println(Hex.encodeHex((byte[]) fids[1]));
		
		System.out.println(verifyMd5((byte[]) fids[1],"1000:771585542c3b3f5e3d7ba67ec60f2ca790ddcb82881d022ab4ce60e684321969:2997ed9c153fc8d406011fbab5e73c97017c32289c5e714b3953bffb1b822000a4e01932dcc3071fde2cc71c9b763663f915a5b2dcf401569fe9af2ba6b3bf57"));

	}

	public static String genUsrCki(String id, String shadow) throws Exception {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] key = md5.digest(CookieUtil.USER_SALT.getBytes());
		byte[] sd = md5.digest(shadow.getBytes());
		byte[] txt = new byte[8 + sd.length];
		System.arraycopy(NumberByte.long2Byte(Long.valueOf(id)), 0, txt, 0, 8);
		System.arraycopy(sd, 0, txt, 8, sd.length);
		byte[] cipher = AESCoder.encrypt(txt, key);
		return URLEncoder.encode(new String(Base64.encodeBase64String(cipher)),"utf8");
	}

	public static Object[] gotUsr(String cookie) throws Exception {
		byte[] src = Base64.decodeBase64(cookie);
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] key = md5.digest(CookieUtil.USER_SALT.getBytes());
		byte[] text = AESCoder.decrypt(src, key);
		byte[] a = new byte[8];
		byte[] b = new byte[text.length - 8];
		System.arraycopy(text, 0, a, 0, 8);
		System.arraycopy(text, 8, b, 0, b.length);
		Object[] r = new Object[2];
		r[0] = String.valueOf(NumberByte.byte2Long(a));
		r[1] = b;
		return r;
	}
	
	public static boolean verifyMd5(byte[] md5,String s)throws Exception{
		MessageDigest md = MessageDigest.getInstance("MD5");
		return StringUtils.equals(new String(md5), new String(md.digest(s.getBytes())));
	}

}
