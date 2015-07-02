package com.blackcrystalinfo.platform.util.cryto;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class DES {

	private final static String DES = "DES/ECB/NoPadding";

	public static void main(String[] args) throws Exception {
		String data = "3zX7k8/nJs9PM0RGdiqoaQ==";
		BASE64Decoder decoder = new BASE64Decoder();
		BASE64Encoder encoder = new BASE64Encoder();
		byte[] d2 = decoder.decodeBuffer(data);
		// byte[] d2 = ByteUtil.fromHex("1bb8a518a94218ace05c0ac2513e27a5");

		System.out.println(ByteUtil.toHex(d2));
		byte[] key = ByteUtil.fromHex("543bd41e2c9f10c7");
		// System.out.println(Long.parseLong(key,16));
		System.err.println(data);
		System.err.println(ByteUtil.toHex(key));
		
		BigInteger inte = new BigInteger(key);
//		System.out.println(inte);
		System.err.println(ByteUtil.toHex(encrypt("{idn:0123456789}".getBytes(), ByteUtil.reverse(key))));

		System.out.println("---------------"+encoder.encode(encrypt("{idn:0123456789}".getBytes(), ByteUtil.reverse(key))));
		
		System.err.println(new String(decrypt(d2, key)));

	}

	/**
	 * Description 根据键值进行加密
	 * 
	 * @param data
	 * @param key
	 *            加密键byte数组
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(String data, String key) throws Exception {
		byte[] bt = encrypt(data.getBytes(), key.getBytes());
		String strs = new BASE64Encoder().encode(bt);
		return strs;
	}

	/**
	 * Description 根据键值进行解密
	 * 
	 * @param data
	 * @param key
	 *            加密键byte数组
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static String decrypt(String data, String key) throws IOException, Exception {
		if (data == null)
			return null;
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] buf = decoder.decodeBuffer(data);
		byte[] bt = decrypt(buf, key.getBytes());
		return new String(bt);
	}

	/**
	 * Description 根据键值进行加密
	 * 
	 * @param data
	 * @param key
	 *            加密键byte数组
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt2(byte[] data, byte[] key) throws Exception {
		// 生成一个可信任的随机数源
		SecureRandom sr = new SecureRandom();

		// 从原始密钥数据创建DESKeySpec对象
		DESKeySpec dks = new DESKeySpec(key);

		// 创建一个密钥工厂，然后用它把DESKeySpec转换成SecretKey对象
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
		SecretKey securekey = keyFactory.generateSecret(dks);

		// Cipher对象实际完成加密操作
		Cipher cipher = Cipher.getInstance(DES);

		// 用密钥初始化Cipher对象
		cipher.init(Cipher.ENCRYPT_MODE, securekey, sr);

		return cipher.doFinal(data);
	}

	/**
	 * Description 根据键值进行解密
	 * 
	 * @param data
	 * @param key
	 *            加密键byte数组
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt2(byte[] data, byte[] key) throws Exception {

		System.out.println("key----------:" + ByteUtil.toHex(key));
		System.out.println("data----------:" + ByteUtil.toHex(data));
		// 生成一个可信任的随机数源
		SecureRandom sr = new SecureRandom();

		// 从原始密钥数据创建DESKeySpec对象
		DESKeySpec dks = new DESKeySpec(key);

		// 创建一个密钥工厂，然后用它把DESKeySpec转换成SecretKey对象
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
		SecretKey securekey = keyFactory.generateSecret(dks);

		// Cipher对象实际完成解密操作
		Cipher cipher = Cipher.getInstance(DES);

		// 用密钥初始化Cipher对象
		cipher.init(Cipher.DECRYPT_MODE, securekey, sr);

		return cipher.doFinal(data);
	}

	public static byte[] encrypt(byte[] content, byte[] key) {
		try {
			int length = content.length;
			byte j = (byte) (8 - (length % 8));
			if (j > 0 && j < 8) {
				j += 8;
				byte temp[] = new byte[length + j];
				for (int i = length; i < temp.length; i++) {
					temp[i] = j;
				}
				System.arraycopy(content, 0, temp, 0, content.length);
				;
				content = temp;
			}
			
			for(int i =0 ;i<key.length;i++){
				key[i] &= 0x7f;
			}
			SecureRandom random = new SecureRandom();
			DESKeySpec desKey = new DESKeySpec(key);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey securekey = keyFactory.generateSecret(desKey);
			Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, securekey, random);
			byte[] result = cipher.doFinal(content);
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * DES解密
	 * 
	 * @param content
	 *            待解密内容
	 * @param key
	 *            解密的密钥
	 * @return
	 */
	public static byte[] decrypt(byte[] content, byte[] key) {
		try {
			for(int i =0 ;i<key.length;i++){
				key[i] &= 0x7f;
			}
			SecureRandom random = new SecureRandom();
			DESKeySpec desKey = new DESKeySpec(key);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey securekey = keyFactory.generateSecret(desKey);
			Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, securekey, random);
			byte[] result = cipher.doFinal(content);
			byte j = result[result.length - 1];
			if (j > 0 && j < 16) {
				byte temp[] = new byte[j];
				System.arraycopy(result, result.length - j, temp, 0, j);
				for (byte b : temp) {
					if (b != j) {
						return result;
					}
				}
				temp = new byte[result.length - j];
				System.arraycopy(result, 0, temp, 0, temp.length);
				result = temp;
			}
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
}