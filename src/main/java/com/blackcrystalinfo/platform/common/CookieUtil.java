package com.blackcrystalinfo.platform.common;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Hashtable;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.lang.StringUtils;

import com.blackcrystalinfo.platform.util.cryto.AESCoder;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

/**
 * 
 * @author j
 * 
 */
public class CookieUtil {
	public static final Hashtable<String,byte[]> USR_KEY = new Hashtable<String,byte[]>();
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

	public static byte[] genDvCki(byte[] mac) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKey secret = new SecretKeySpec(DEVICE_SALT.getBytes(), "HMACSHA256");
		hmac.init(secret);
		byte[] doFinal = hmac.doFinal(mac);
		return doFinal;
	}

	public static boolean isDvCki(byte[] mac, String cookie) throws Exception {
		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKey secret = new SecretKeySpec(DEVICE_SALT.getBytes(), "HMACSHA256");
		hmac.init(secret);
		byte[] doFinal = hmac.doFinal(mac);
		System.out.println(Hex.encodeHexString(doFinal));
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

	public static byte[] gen16() {
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
		byte[] b = gen16();
		System.out.println(Hex.encodeHexString(b));
		byte[] ctn = new byte[8];
		EndianUtils.writeSwappedLong(ctn, 0, -1L);
		System.out.println(Hex.encodeHexString(ctn));

//		String s = CookieUtil.gotUserIdFromCookie("OXwzMDB8MTQ0ODAwNjMyNjA5N3xlNjhjZTcxZjcwODU5YmRmZmYzMDIzOGFjYjA1ODcxMWYzZWQ4Yg==-D49ABECA71C325905426D5795257E500");
//		System.out.println(s);

		System.out.println("---------------------DEV-COOKIE----------------------------");
//		byte[] licenseKey =  new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };
		byte[] licenseKey =  new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
//		byte[] mac = Hex.decodeHex("0000b0d59d63f86a".toCharArray());
		byte[] mac = Hex.decodeHex("0000b0d59d63f58a".toCharArray());
		System.out.println(Hex.encodeHexString(mac));
		byte[] dvcookie = CookieUtil.genDvCki(mac);
		System.out.println(dvcookie.length);
		System.out.println("原串:"+Hex.encodeHexString(dvcookie));

		byte[] licenseKeyCookie = new byte[dvcookie.length + licenseKey.length];
		
		System.arraycopy(dvcookie, 0, licenseKeyCookie, 0, 32);
		System.arraycopy(licenseKey, 0, licenseKeyCookie, 32, 16);
		
		byte[] keyMd5 = MessageDigest.getInstance("MD5").digest(licenseKeyCookie);
		byte[] cookieCipher = AESCoder.encryptNp(dvcookie, licenseKey);
		byte[] cookieCipher2 = AESCoder.encryptNp(dvcookie, keyMd5);
		
		System.out.println("注册:"+Hex.encodeHexString(cookieCipher));
		System.out.println("登录:"+Hex.encodeHexString(cookieCipher2));
		
		
		
		System.out.println("---------------------USR-COOKIE----------------------------");
		byte[] k = gen16();
		String usrCki = genUsrCki("100","1000:771585542c3b3f5e3d7ba67ec60f2ca790ddcb82881d022ab4ce60e684321969:2997ed9c153fc8d406011fbab5e73c97017c32289c5e714b3953bffb1b822000a4e01932dcc3071fde2cc71c9b763663f915a5b2dcf401569fe9af2ba6b3bf57",k);
		System.out.println(usrCki);

//		Object[] fids = gotUsr(URLDecoder.decode(usrCki,"utf8"),k);
		Object[] fids = gotUsr(usrCki,k);
//		Object[] fids = gotUsr("zRJ743WVzw7bSjAyC4AowXv240LD%2BNGgxbS3Mri93fg%3D",k);
		System.out.println((String) fids[0]);
		System.out.println(Hex.encodeHex((byte[]) fids[1]));
		
		System.out.println(verifyMd5((byte[]) fids[1],"1000:771585542c3b3f5e3d7ba67ec60f2ca790ddcb82881d022ab4ce60e684321969:2997ed9c153fc8d406011fbab5e73c97017c32289c5e714b3953bffb1b822000a4e01932dcc3071fde2cc71c9b763663f915a5b2dcf401569fe9af2ba6b3bf57"));

		
		System.out.println(URLDecoder.decode("6SZ2iy8wW7N8gAJuMzYKQLZo7gS1xcXjOl6B5hFqoL8%3D", "utf8"));
		System.out.println(URLEncoder.encode("zRJ743WVzw7bSjAyC4AowXv240LD+NGgxbS3Mri93fg=", "utf8"));
		
		
//		boolean id=CookieUtil.isDvCki(Hex.decodeHex("00002091489838a2".toCharArray()), "3301e846c35bf652bcf2b659147f08190a6ca84f1d586eab00e2c16a7f5d963d");
//		System.out.println("isDvCki:"+id);
		byte[] ckia = AESCoder.encryptNp(ByteUtil.fromHex("76d10eabcd2dc37ce8373e1f73ff246d9a633f2d322120107eff386367919ed6"), ByteUtil.fromHex("faaf34b5f7dc0c5d2f134430e16a3807"));
		System.out.println("ckia:"+ByteUtil.toHex(ckia));
		byte[] ckitxt=AESCoder.decryptNp(Hex.decodeHex("3301e846c35bf652bcf2b659147f08190a6ca84f1d586eab00e2c16a7f5d963d".toCharArray()),ByteUtil.fromHex("faaf34b5f7dc0c5d2f134430e16a3807"));
		System.out.println("ckip:"+ByteUtil.toHex(ckitxt));
		boolean id=CookieUtil.isDvCki(Hex.decodeHex("00002091489838a2".toCharArray()), ByteUtil.toHex(ckitxt));
		System.out.println("isDvCki:"+id);
		byte[] dvc =CookieUtil.genDvCki(Hex.decodeHex("030027430c000000".toCharArray()));
		System.out.println(Hex.encodeHexString(dvc));
	}

	public static String genUsrCki(String id, String shadow,byte[] salt) throws Exception {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] sd = md5.digest(shadow.getBytes());
		byte[] txt = new byte[8 + sd.length];
		System.arraycopy(NumberByte.long2Byte(Long.valueOf(id)), 0, txt, 0, 8);
		System.arraycopy(sd, 0, txt, 8, sd.length);
		byte[] cipher = AESCoder.encrypt(txt, salt);
		return URLEncoder.encode(new String(Base64.encodeBase64String(cipher)),"utf8");
	}

	public static Object[] gotUsr(String cookie,byte[] salt) throws Exception {
		byte[] src = Base64.decodeBase64(URLDecoder.decode(cookie,"utf8"));
		byte[] text = AESCoder.decrypt(src, salt);
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
