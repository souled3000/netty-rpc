package com.blackcrystalinfo.platform.util;

import java.util.Random;

public final class VerifyCode {
	final static char[] src = new String("abcdefghijklmnopqrstuvwxyz" + "0123456789").toCharArray();

	public final static String randString(int length) {
		Random r = new Random();
		char[] buf = new char[length];
		int rnd;
		for (int i = 0; i < length; i++) {
			rnd = Math.abs(r.nextInt()% src.length) ;

			buf[i] = src[rnd];
		}
		return new String(buf);
	}
	public static void main(String[] args) {
		long l = System.currentTimeMillis();
		for(int n = 0 ; n++<100000000;){
			VerifyCode.randString(6);
		}
		System.out.println((System.currentTimeMillis()-l)/1000);
	}
}
