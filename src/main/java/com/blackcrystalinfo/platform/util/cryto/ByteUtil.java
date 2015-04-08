package com.blackcrystalinfo.platform.util.cryto;

/**
 * 创建人：李春江
 * 创建日期：Oct 13, 2011 8:36:52 PM
 * 说明：
 */

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.commons.lang.StringUtils;

import sun.misc.BASE64Encoder;

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

	/**
	 * 将byte数组变为可显示的字符串
	 * 
	 * @param b
	 * @return
	 */
	public static String byte2String(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length + b.length - 1);
		for (int i = 0; i < b.length; i++) {
			sb.append(b[i]);
			if (i < b.length - 1)
				sb.append(",");
		}
		return sb.toString();
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

	public static void main(String[] args) {
		String s = "938310a2bcf7111";
		byte[] bb = reverse(ByteUtil.fromHex(s));
		System.out.println(ByteUtil.toHex(bb));

		System.out.println(crc("{idn:\"01292838\"}"));

		System.out.println("9bT4cGXR/7/OuPrFTVrVWHZJik4aQyLW310g/TM7s3H35jDLvgkkXq/9HPHPQRVVg5sCQ9Cy7Vxt".length());

		s = "{\"cookie\":\"-5|FjQL3sHhII0BprdPdutBvKLb1vbkIaMBVeLQc2RshKY=\",\"status\":0,\"statusMsg\":\"\"}";

		BASE64Encoder encoder = new BASE64Encoder();

		String ss = encoder.encode(s.getBytes());
		System.out.println(encoder.encode(s.getBytes()));
		System.out.println(encoder.encodeBuffer(s.getBytes()));
		System.out.println(StringUtils.escape(encoder.encode(s.getBytes())));
		System.out.println(StringUtils.remove(StringUtils.escape(encoder.encode(s.getBytes())), "\\n"));
		System.out.println(ss.replaceAll("\\s", ""));

		System.out.println();

		byte[] src = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };
		System.out.println(src.length);
		System.out.println(ByteUtil.reverse(src));
		
		Map m = new HashMap();
		m.put("status", 1);
		System.out.println(writeJSON(m));
	}

	public static byte[] reverse(byte[] src) {
		ByteBuffer bb = ByteBuffer.allocate(src.length);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.put(src);
		bb.slice();
		return ByteUtil.fromHex(Long.toHexString(bb.getLong(0)));
	}
}
