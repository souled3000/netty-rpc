package com.blackcrystalinfo.platform.util;

import java.io.IOException;
import java.math.BigInteger;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class StringUtil {
	public static byte[] fromHex(String hex) {
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return bytes;
	}

	public static String toHex(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		String hex = bi.toString(16);
		int paddingLength = (bytes.length * 2) - hex.length();
		if (paddingLength > 0) {
			return String.format("%0" + paddingLength + "d", 0) + hex;
		} else {
			return hex;
		}
	}

	public static String base64Encode(byte[] bs) {
		return new BASE64Encoder().encode(bs);
	}

	public static byte[] base64Decode(String str) throws IOException {
		return new BASE64Decoder().decodeBuffer(str);
	}

	public static byte[] mac2Byte(String mac) throws IOException {
		return base64Decode(mac.replace(' ', '+'));
	}

	public static String mac2Base64(byte[] mac) throws IOException {
		return base64Encode(mac);
	}

	public static void main(String[] args) {
		String str = "KyuqJk6cAAA=";
		try {
			byte[] bs = StringUtil.base64Decode(str);
			System.out.println(toHex(bs));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
