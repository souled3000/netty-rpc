package com.blackcrystalinfo.platform.util.cryto;

import java.util.Random;

public final class RandomStr {
//	final static char[] src = new String("abcdefghijklmnopqrstuvwxyz" + "0123456789"+"~!@#$%^&*()_+-={}[]\":;',./<>?'").toCharArray();
	final static char[] src = new String("abcdefghijklmnopqrstuvwxyz" + "0123456789").toCharArray();

	public final static String randString(int length) {
		Random r = new Random();
		char[] buf = new char[length];
		int rnd;
		for (int i = 0; i < length; i++) {
			rnd = Math.abs(r.nextInt() % src.length);

			buf[i] = src[rnd];
		}
		return new String(buf);
	}
	public static void main(String[] args) {
		long l = System.currentTimeMillis();
		int x =0;
		for(int n = 0 ; n++<100000000;){
//			RandomStr.randString(16);
			x++;
		}
		System.out.println(x);
		System.out.println((System.currentTimeMillis()-l));
		System.out.println((System.currentTimeMillis()-l)/1000);
	}
	public static void main2(String[] args) {
		for(int n = 0 ; n <255 ; n++){
			System.out.println(RandomStr.randString(16));
		}
	}
}
