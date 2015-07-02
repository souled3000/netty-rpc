package com.blackcrystalinfo.platform.util;

import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.springframework.beans.factory.FactoryBean;

public class PasswordDecoder implements FactoryBean<String> {
	private String code;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String getObject() throws Exception {
		String pwd = decrypt(this.code);
		return pwd;
	}

	@Override
	public Class<?> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private static final String rawKey = Constants.DB_PWD_RAWKEY;
	private static final String PWALG_SIMPLE_STRING = "0123456789ABCDEF";
	private static final int PWALG_SIMPLE_MAXLEN = 30;

	public static String encrypt(String password) {
		String str = null;
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(new DESKeySpec(rawKey
					.getBytes()));

			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, key, new SecureRandom());

			byte[] src = password.getBytes();
			byte[] code = cipher.doFinal(src);

			str = toStringValue(code);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return str;
	}

	private static String toStringValue(byte[] code) {
		String result = "";
		char[] chars = PWALG_SIMPLE_STRING.toCharArray();
		Random rdm = new Random();
		int len = code.length;
		int shift = (len < PWALG_SIMPLE_MAXLEN) ? rdm
				.nextInt(PWALG_SIMPLE_MAXLEN - len) : 0;

		result += hexConvert((byte) shift, chars);
		result += hexConvert((byte) len, chars);

		for (int i = 0; i < shift; i++)
			result += hexConvert((byte) rdm.nextInt(256), chars);

		for (int i = 0; i < len; i++)
			result += hexConvert(code[i], chars);

		while (result.length() < PWALG_SIMPLE_MAXLEN * 2)
			result += hexConvert((byte) rdm.nextInt(256), chars);

		return result;
	}

	private static String hexConvert(byte b, char[] chars) {
		char hight = chars[(b & 0xF0) >> 4];
		char low = chars[b & 0x0F];
		return new String(new char[] { hight, low });
	}

	public static String decrypt(String str) {
		String password = null;
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(new DESKeySpec(rawKey
					.getBytes()));

			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE, key, new SecureRandom());

			byte[] code = toByteArray(str);
			byte[] src = cipher.doFinal(code);

			password = new String(src);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return password;
	}

	private static byte[] toByteArray(String str) {
		char[] chars = str.toCharArray();
		int idx = binConvert(0, chars) * 2;
		int len = binConvert(2, chars);
		byte[] result = new byte[len];

		for (int j = 0, i = idx + 4; j < len; i = i + 2)
			result[j++] = binConvert(i, chars);

		return result;
	}

	private static byte binConvert(int i, char[] chars) {
		byte hight = (byte) (PWALG_SIMPLE_STRING.indexOf(chars[i]) << 4);
		byte low = (byte) PWALG_SIMPLE_STRING.indexOf(chars[++i]);
		return (byte) (hight + low);
	}

	public static void main(String[] args) {
		String password = "smarthome123";

		String code = encrypt(password);

		System.out.println(code);

		System.out.println(decrypt(code));

	}
}
