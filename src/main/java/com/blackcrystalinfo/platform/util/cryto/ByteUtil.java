package com.blackcrystalinfo.platform.util.cryto;

/**
 * 创建人：李春江
 * 创建日期：Oct 13, 2011 8:36:52 PM
 * 说明：
 */

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;

/**
 */
public class ByteUtil {
	private static String digits = "0123456789abcdef";

	/**
	 * 将byte数组变为可显示的字符串
	 * 
	 * @param b
	 * @return
	 */
	public static String toHex(byte[] data) {

		StringBuffer buf = new StringBuffer();

		for (int i = 0; i != data.length; i++) {
			int v = data[i] & 0xff;

			buf.append(digits.charAt(v >> 4));
			buf.append(digits.charAt(v & 0xf));
		}

		return buf.toString().toUpperCase();
	}

	public static byte[] fromHex(String hex) {
		if (hex.length() % 2 > 0)
			hex = "0" + hex;
		ByteBuffer bf = ByteBuffer.allocate(hex.length() / 2);
		for (int i = 0; i < hex.length(); i++) {
			String hexStr = hex.charAt(i) + "";
			i++;
			hexStr += hex.charAt(i);
			byte b = (byte) Integer.parseInt(hexStr, 16);
			// System.out.println(hexStr+"="+b);
			bf.put(b);
		}
		return bf.array();
	}

	public static long crc32(String src, String checksum) {
		CRC32 crc = new CRC32();
		crc.update(src.getBytes());
		Long checksum2 = crc.getValue();
		BigInteger bi = new BigInteger(ByteUtil.fromHex(checksum));
		return bi.longValue() - checksum2;
	}

	public static String crc(String src) {
		CRC32 crc = new CRC32();
		crc.update(src.getBytes());
		Long checksum = crc.getValue();
		System.out.println("checksum:" + checksum);
		return Long.toHexString(checksum);
	}

	public static String writeJSON(Object t) {
		SerializeWriter out = new SerializeWriter();
		try {
			JSONSerializer serializer = new JSONSerializer(out);
			// serializer.config(SerializerFeature.QuoteFieldNames, false);
			// serializer.config(SerializerFeature.UseSingleQuotes, true);
			serializer.write(t);
			return out.toString();
		} finally {
			out.close();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {
		byte[] src = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D };
		System.out.println(ByteUtil.toHex(src));

		System.out.println(crc("{idn:\"01292838\"}"));

		System.out.println(ByteUtil.toHex(src));

		Map m = new HashMap();
		m.put("status", 1);
		System.out.println(writeJSON(m));
	}

}
