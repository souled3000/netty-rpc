package com.blackcrystalinfo.platform.common;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PBKDF2 {

	public static String encode(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
		int iterations = 1000;
		char[] chars = str.toCharArray();
		byte[] salt = getSalt();

		PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = skf.generateSecret(spec).getEncoded();
		return iterations + ":" + StringUtil.toHex(salt) + ":" + StringUtil.toHex(hash);
	}

	public static boolean validate(String rawStr, String codeStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String[] parts = codeStr.split(":");
		int iterations = Integer.parseInt(parts[0]);
		byte[] salt = StringUtil.fromHex(parts[1]);
		byte[] hash = StringUtil.fromHex(parts[2]);

		PBEKeySpec spec = new PBEKeySpec(rawStr.toCharArray(), salt, iterations, hash.length * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] testHash = skf.generateSecret(spec).getEncoded();

		int diff = hash.length ^ testHash.length;
		for (int i = 0; i < hash.length && i < testHash.length; i++) {
			diff |= hash[i] ^ testHash[i];
		}
		return diff == 0;
	}

	private static byte[] getSalt() throws NoSuchAlgorithmException {
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt;
	}

	public static void main(String[] args) throws Exception {
		String password = "lchj";
		System.out.println(PBKDF2.encode(password));
		System.out.println(PBKDF2.encode(password));

		String pwd = "xxx";
		String p = "yyy";
		boolean b = PBKDF2.validate(p, PBKDF2.encode(pwd));
		System.out.println(b);
	}
}
