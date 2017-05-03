package com.blackcrystalinfo.platform.common;

public final class NumberByte {

	public static byte[] short2Byte(short x) {
		byte[] bb = new byte[2];
		bb[0] = (byte) (x >> 8);
		bb[1] = (byte) (x >> 0);
		return bb;
	}
	public static byte[] long2Byte(long x) {
		byte[] bb = new byte[8];
		bb[0] = (byte) (x >> 56);
		bb[1] = (byte) (x >> 48);
		bb[2] = (byte) (x >> 40);
		bb[3] = (byte) (x >> 32);
		bb[4] = (byte) (x >> 24);
		bb[5] = (byte) (x >> 16);
		bb[6] = (byte) (x >> 8);
		bb[7] = (byte) (x >> 0);
		return bb;
	}

	public static long byte2Long(byte[] bb) {
		return ((((long) bb[0] & 0xff) << 56) | (((long) bb[1] & 0xff) << 48) | (((long) bb[2] & 0xff) << 40) | (((long) bb[3] & 0xff) << 32) | (((long) bb[4] & 0xff) << 24) | (((long) bb[5] & 0xff) << 16) | (((long) bb[6] & 0xff) << 8) | (((long) bb[7] & 0xff) << 0));
	}

	public static byte[] int2Byte(int i) {
		byte[] result = new byte[4];
		result[0] = (byte) ((i >> 24) & 0xFF);
		result[1] = (byte) ((i >> 16) & 0xFF);
		result[2] = (byte) ((i >> 8) & 0xFF);
		result[3] = (byte) (i & 0xFF);
		return result;
	}

	public static int byte2Int(byte[] src) {
		int offset = 0;
		int value;
		value = (int) ((src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8) | ((src[offset + 2] & 0xFF) << 16) | ((src[offset + 3] & 0xFF) << 24));
		return value;
	}

	public static long byte2LongLittleEndian(byte[] src) {
		int offset = 0;
		long value;
		value = (long) (src[offset] & 0xFF) | ((long) (src[offset + 1] & 0xFF) << 8) | ((long) (src[offset + 2] & 0xFF) << 16) | ((long) (src[offset + 3] & 0xFF) << 24) | ((long) (src[offset + 4] & 0xFF) << 32)
				| ((long) (src[offset + 5] & 0xFF) << 40) | ((long) (src[offset + 6] & 0xFF) << 48) | ((long) (src[offset + 7] & 0xFF) << 56);
		return value;
	}
	public static byte[] long2ByteLittleEndian(long x) {
		byte[] bb = new byte[8];
		bb[7] = (byte) (x >> 56);
		bb[6] = (byte) (x >> 48);
		bb[5] = (byte) (x >> 40);
		bb[4] = (byte) (x >> 32);
		bb[3] = (byte) (x >> 24);
		bb[2] = (byte) (x >> 16);
		bb[1] = (byte) (x >> 8);
		bb[0] = (byte) (x >> 0);
		return bb;
	}

	public static void main(String[] args) {
//		Long l = System.currentTimeMillis();
//		System.out.println(ByteUtil.toHex(NumberByte.long2Byte(l)));
//		System.out.println(Long.toHexString(l).toUpperCase());
//		System.out.println(byte2Int(int2Byte(-1)));
//
//		System.out.println(ByteUtil.toHex(long2Byte(-4)));
//		System.out.println(byte2Long(long2Byte(-4)));
		f3();
	}
	public static void f3(){
		Long x = Long.MAX_VALUE;
		byte[] y =NumberByte.long2Byte(x);
		System.out.println(org.apache.commons.codec.binary.Hex.encodeHexString(y));
		System.out.println(NumberByte.byte2Long(y));
		System.out.println(Long.MAX_VALUE);
		
		
	}
}
