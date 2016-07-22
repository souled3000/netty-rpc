package com.blackcrystalinfo.platform.common;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class PBKDF2 {

	public static String encode(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
		int iterations = 1000;
		char[] chars = str.toCharArray();
		byte[] salt = getSalt();

		PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = skf.generateSecret(spec).getEncoded();
		return iterations + ":" + ByteUtil.toHex(salt) + ":" + ByteUtil.toHex(hash);
	}

	public static boolean validate(String rawStr, String codeStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String[] parts = codeStr.split(":");
		int iterations = Integer.parseInt(parts[0]);
		byte[] salt = ByteUtil.fromHex(parts[1]);
		byte[] hash = ByteUtil.fromHex(parts[2]);

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
		byte[] salt = new byte[32];
		sr.nextBytes(salt);
		return salt;
	}

	public static void main(String[] args) throws Exception {
		String pwd = "1111";
		System.out.println(PBKDF2.encode(pwd));
		System.out.println(PBKDF2.encode(pwd));

		String p = "yyy";
		boolean b = PBKDF2.validate(pwd, PBKDF2.encode(pwd));
		System.out.println(b);
		b=PBKDF2.validate("lchj", "1000:9E33B2CE4AD4F870A56840BCD1D53BE2D52B7A0383424700AAC9F907F2C3ECC3:F31C4F59A48418325BDA99355A675CED406FB769F1BD53404C7D4304EFD2B5BDD2524203E499BDDF0FCFDFFDCAD086FE03699891A0BE464ECDD6FF14B08026BC");
		System.out.println(b);
		System.out.println(Hex.encodeHex(getSalt()));
	}
}
