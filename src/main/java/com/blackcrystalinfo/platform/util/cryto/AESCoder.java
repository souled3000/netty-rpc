package com.blackcrystalinfo.platform.util.cryto;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
/**
 * 算法/模式/填充                    16字节加密后数据长度         不满16字节加密后长度

AES/CBC/NoPadding                  16                         不支持
AES/CBC/PKCS5Padding             32                         16
 AES/CBC/ISO10126Padding        32                          16
 AES/CFB/NoPadding                    16                          原始数据长度
AES/CFB/PKCS5Padding              32                          16
 AES/CFB/ISO10126Padding         32                          16
 AES/ECB/NoPadding                    16                          不支持
AES/ECB/PKCS5Padding              32                          16
 AES/ECB/ISO10126Padding         32                          16
 AES/OFB/NoPadding                     16                          原始数据长度
AES/OFB/PKCS5Padding               32                          16
 AES/OFB/ISO10126Padding          32                          16
 AES/PCBC/NoPadding                   16                          不支持
AES/PCBC/PKCS5Padding             32                          16
 AES/PCBC/ISO10126Padding        32                          16

 * @author Taylor
 *
 */
public abstract class AESCoder {

	/**
	 * 密钥算法
	 */
	public static final String KEY_ALGORITHM = "AES";

	/**
	 * 加密/解密算法 / 工作模式 / 填充方式 
	 * Java 6支持PKCS5Padding填充方式 
	 * Bouncy Castle支持PKCS7Padding填充方式
	 */
	public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

	/**
	 * 转换密钥
	 * 
	 * @param key 二进制密钥
	 * @return Key 密钥
	 * @throws Exception
	 */
	private static Key toKey(byte[] key) throws Exception {

		// 实例化AES密钥材料
		SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);

		return secretKey;
	}

	public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
		Key k = toKey(key);
		Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, k);
		return cipher.doFinal(data);
	}

	public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
		Key k = toKey(key);
		Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, k);
		return cipher.doFinal(data);
	}

	/**
	 * 生成密钥 <br>
	 * 
	 * @return byte[] 二进制密钥
	 * @throws Exception
	 */
	public static byte[] initKey() throws Exception {

		// 实例化
		KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);

		/*
		 * AES 要求密钥长度为 128位、192位或 256位
		 */
		kg.init(128);

		// 生成秘密密钥
		SecretKey secretKey = kg.generateKey();

		// 获得密钥的二进制编码形式
		return secretKey.getEncoded();
	}
	public static byte[] decryptNp(byte[] data, byte[] key) throws Exception {
		Key k = toKey(key);
		Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, k);
		return cipher.doFinal(data);
	}
	public static byte[] encryptNp(byte[] data, byte[] key) throws Exception {
		Key k = toKey(key);
		Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, k);
		return cipher.doFinal(data);
	}
	
	public static void main(String[] args) throws Exception{
		byte[] a = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] x = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 ,0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		byte[] b = AESCoder.encryptNp(x, a);
		System.out.println(Hex.encodeHexString(b));
		byte[] c = AESCoder.decryptNp(b, a);
		System.out.println(Hex.encodeHexString(c));
	}
}
