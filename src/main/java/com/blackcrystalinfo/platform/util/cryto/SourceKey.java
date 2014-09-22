package com.blackcrystalinfo.platform.util.cryto;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class SourceKey {
//	public final static String gen() throws NoSuchAlgorithmException {
		
		// String str = RandomStr.randString(32);
//		String timestamp = String.valueOf(System.currentTimeMillis());
//		System.out.println(timestamp);
		// String md5_buf = String.format("%s%s", str, timestamp);
		// byte[] bytes = MessageDigest.getInstance("MD5").digest(md5_buf.getBytes());
		//
		// System.out.println(bytes.length);
		// System.out.println(StringUtil.toHex(bytes));

//		BASE64Encoder encoder = new BASE64Encoder();
//		String md5Str = encoder.encode(timestamp.getBytes());

//		return md5Str;
//	}

	public static void main(String[] args) throws NoSuchAlgorithmException, Exception {
		String hex = gen();
		byte[] f1 = ByteUtil.fromHex(hex);
		byte[] f2 = new byte[8];
		System.arraycopy(f1, 0, f2, 0, 8);
		
		Long  f3 = Long.parseLong(ByteUtil.toHex(f2),16);
		System.out.println(ByteUtil.toHex(f1));
		System.out.println(f3);
		
		 byte[] bytes = MessageDigest.getInstance("MD5").digest(f1);
		 System.out.println(ByteUtil.toHex(bytes));
	}
	
	public static String gen() throws Exception{
		Long millis = System.currentTimeMillis();
		Long nano = System.nanoTime();
		byte[] bKey = new byte[16];
		BigDecimal b1= new BigDecimal(millis);
		BigDecimal b2 = new BigDecimal(nano);
		System.arraycopy(b1.toBigInteger().toByteArray(), 0,bKey , 8-b1.toBigInteger().toByteArray().length, b1.toBigInteger().toByteArray().length);
		System.arraycopy(b2.toBigInteger().toByteArray(), 0,bKey , 8-b2.toBigInteger().toByteArray().length+8, b2.toBigInteger().toByteArray().length);
		String total = ByteUtil.toHex(bKey);
		return total;
	}
}
