package com.blackcrystalinfo.platform.util;


import java.math.BigInteger;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class MiscUtils {
	public static byte[] fromHex(String hex) {
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return bytes;
	}

	public static String toHex(byte... bytes) {
		if(bytes==null||bytes.length==0){
			return "";
		}
		BigInteger bi = new BigInteger(1, bytes);
		String hex = bi.toString(16);
		int paddingLength = (bytes.length * 2) - hex.length();
		if (paddingLength > 0) {
			return String.format("%0" + paddingLength + "d", 0) + hex;
		} else {
			return hex;
		}
	}

	public static String toHex(byte[] bytes, int b, int e) {
		byte[] aim = new byte[e - b];
		System.arraycopy(bytes, b, aim, 0, e - b);

		BigInteger bi = new BigInteger(1, aim);
		String hex = bi.toString(16);
		int paddingLength = (aim.length * 2) - hex.length();
		if (paddingLength > 0) {
			return String.format("%0" + paddingLength + "d", 0) + hex;
		} else {
			return hex;
		}
	}

	public static String writeJSON(Object t, String... ss) {
		SerializeWriter out = new SerializeWriter();
		try {
			JSONSerializer serializer = new JSONSerializer(out);
			// serializer.config(SerializerFeature.QuoteFieldNames, false);
			// serializer.config(SerializerFeature.UseSingleQuotes, true);

			for (String s : ss) {
				final String x = s;
				serializer.getPropertyFilters().add(new PropertyFilter() {
					public boolean apply(Object obj, String s, Object obj1) {
						if (s.equals(x))
							return false;
						return true;
					}
				});
			}

			serializer.write(t);
			return out.toString();
		} finally {
			out.close();
		}
	}

	public static String writeJSON(Object t) {
		SerializeWriter out = new SerializeWriter();
		try {
			JSONSerializer serializer = new JSONSerializer(out);
			serializer.config(SerializerFeature.QuoteFieldNames, false);
			serializer.config(SerializerFeature.UseSingleQuotes, true);
			serializer.write(t);
			return out.toString();
		} finally {
			out.close();
		}
	}

	public static int copyright(String cp) {
		String[] ns = cp.split("\\.");
		int n = 0;
		int p = 1000000000;
		for (String s : ns) {
			System.out.println(s);
			n += (Integer.parseInt(s) * (p /= 100));
			System.out.println(n);

		}
		return n;
	}

	public static byte crc(byte[] ctn) {
		int headerCheck = 0;
		for (int i = 0; i < ctn.length; i++) {
			headerCheck ^= ctn[i];
			for (int n = 8; n > 0; n--) {
				if ((headerCheck & 0x80) != 0) {
					headerCheck = (headerCheck << 1) ^ 0x31;
				} else {
					headerCheck = (headerCheck << 1);
				}
			}
		}
		return (byte) headerCheck;
	}

	public static byte crc(byte[] ctn, int l) {
		int headerCheck = 0;
		for (int i = 0; i < l; i++) {
			headerCheck ^= ctn[i];
			for (int n = 8; n > 0; n--) {
				if ((headerCheck & 0x80) != 0) {
					headerCheck = (headerCheck << 1) ^ 0x31;
				} else {
					headerCheck = (headerCheck << 1);
				}
			}
		}
		return (byte) headerCheck;
	}

	public static byte[] swapBytes(byte[] aim) {
		for (int i = 0; i < aim.length / 2; i++) {
			byte o = aim[i];
			aim[i] = aim[aim.length - 1 - i];
			aim[aim.length - 1 - i] = o;
		}
		return aim;
	}

	public static void main(String[] args) throws Exception {
		f4();
	}

	public static void f3() {
		byte[] a = new byte[] { 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };

		short b = EndianUtils.readSwappedShort(a, 2);


		int c = EndianUtils.readSwappedInteger(a, 4);
		System.out.println(MiscUtils.toHex(NumberByte.int2Byte(c)));
	}

	public static void f4() {
		byte[] a = MiscUtils.fromHex("c2000400f8130ee1f8130ee1f8130ee1e9131fe1e9131fe17e13cfe1c01352bd129468591cd6d49f019364540a67c1eb80826006cb2a686154d510b03a09ce8509f766843eafe44244a01386098d61184145fc0784f0798a840ffdc2c7c17bcf");
		byte[] mask = new byte[] { 0x11, 0x11, 0x11, 0x11 };
		System.out.println(toHex(a));
		// MaskContent(a,mask);
		MaskContent(a, (short) 8, (short) 32, a, 4);
		System.out.println(toHex(a));
		// MaskContent(a,mask);
		MaskContent(a, (short) 8, (short) 32, a, 4);
		System.out.println(toHex(a));
	}

	private static void f2() throws Exception {
		byte[] a = new byte[] { 0x01, 0x02, 0x03 };
		MiscUtils.swapBytes(a);
		System.out.println(MiscUtils.toHex(a));
		a = new byte[] { 0x01, 0x02, 0x03, 0x04 };
		MiscUtils.swapBytes(a);
		System.out.println(MiscUtils.toHex(a));
	}

	private static void testCrc() {
		byte[] test = MiscUtils.fromHex("002200bb11111122110099");
		byte a = crc(test);
		System.out.println(MiscUtils.toHex(new byte[] { a }));
	}

	private static void testRandomKey() {
		short a = 1;
		long b = 2;
		int c = 3;
		byte[] k = RandomKeyGet(a, b, c);
		System.out.println(toHex(k));
		System.out.println(String.format("%x", new byte[] { 0x00, 0x32, -0x71, -0x1 }));

	}

	public static void MaskContent(byte[] payload, byte[] mask) {
		byte desp = mask[0];
		byte mask_index = (byte) 0x00;
		for (short iter = 0; iter < payload.length; iter++) {
			/* rotate mask and apply it */
			mask_index = (byte) ((iter + (short) desp) % 4);
			payload[iter] ^= mask[mask_index];
		} /* end while */
	}


	public static void MaskContent(byte[] payload, short b, short e, byte[] mask, int mb) {
		short desp = (short)(mask[mb]&0xff);
		byte mask_index = (byte) mb;
		for (short i = b; i < e; i++) {
			mask_index = (byte) ((i-b + desp) % 4);
			payload[i] ^= mask[mask_index+mb];
		} /* end while */
	}

	public static byte[] RandomKeyGet(short seq, long dst, int time) {
		byte[] id = new byte[8];
		EndianUtils.writeSwappedLong(id, 0, dst);
		byte[] key = new byte[16];
		byte[] timeArry = new byte[4];
		byte timeXor, i, n;

		EndianUtils.writeSwappedInteger(timeArry, 0, time);
		timeXor = (byte) (timeArry[0] ^ timeArry[1] ^ timeArry[2] ^ timeArry[3]);

		EndianUtils.writeSwappedInteger(key, 0, time);
		System.arraycopy(id, 0, key, 4, 8);
		EndianUtils.writeSwappedShort(key, 12, seq);
		EndianUtils.writeSwappedShort(key, 14, seq);
		for (i = 0; i < ((timeXor & 0x03) + 1); i++) {
			for (n = 0; n < 16; n++) {
				key[n] ^= id[(timeXor + (i + n)) & 0x07] ^ timeArry[((i + n) & 0x03)];
			}
		}
		return key;
	}
}
