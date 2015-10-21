package com.blackcrystalinfo.platform.util.cryto;

import java.math.BigDecimal;
/**
 * 无归属工具类
 * @author Taylor
 *
 */
public final class Misc {
	/**
	 * 用系统毫秒纳秒生成16字节的key
	 * @return
	 * @throws Exception
	 */
	public static byte[] gen() throws Exception {
		Long millis = System.currentTimeMillis();
		Long nano = System.nanoTime();
		byte[] key = new byte[16];
		BigDecimal b1 = new BigDecimal(millis);
		BigDecimal b2 = new BigDecimal(nano);
		System.arraycopy(b1.toBigInteger().toByteArray(), 0, key, 8 - b1.toBigInteger().toByteArray().length, b1.toBigInteger().toByteArray().length);
		System.arraycopy(b2.toBigInteger().toByteArray(), 0, key, 8 - b2.toBigInteger().toByteArray().length + 8, b2.toBigInteger().toByteArray().length);
		return key;
	}
	
}
