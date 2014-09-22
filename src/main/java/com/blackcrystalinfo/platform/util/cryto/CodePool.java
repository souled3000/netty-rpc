package com.blackcrystalinfo.platform.util.cryto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;

public final class CodePool {

	private static List<String> pool = new ArrayList<String>();
	static {
		try {
			File f = new File(System.getProperty("user.dir") + File.separator
					+ "keys");
			if(!f.exists())
				CodePool.flushPool();
			else
			{
				BufferedReader r = new BufferedReader(new FileReader(f));
				String s = "";
				while((s = r.readLine())!=null){
					pool.add(s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final static String gen() throws NoSuchAlgorithmException {
		// String str = RandomStr.randString(32);
		// String timestamp = String.valueOf(System.currentTimeMillis());
		//
		// String md5_buf = String.format("%s%s", str, timestamp);
		// byte[] bytes = MessageDigest.getInstance("MD5").digest(
		// md5_buf.getBytes());

		// return StringUtil.toHex(bytes);
		Random r = new Random();
		Long l = Math.abs((long)r.nextInt());
		return StringUtils.leftPad(Long.toHexString(l), 8, "0");
	}

	public static void flushPool() throws Exception {
		pool.clear();
		File f = new File(System.getProperty("user.dir") + File.separator
				+ "keys");
		PrintWriter pw = new PrintWriter(new FileWriter(f, false));
		for (int n = 0; n < 256; n++) {
			String s = gen();
			pool.add(s);
			pw.write(s + "\n");
		}

		pw.flush();
		pw.close();
	}

	public static void main(String[] args) throws Exception {
		for (String s : pool)
			if(s.length()<8)
			System.out.println(s);

		System.out.println("-----------------------------------------------");
		
		StringBuilder sb = new StringBuilder();
		for(int i =1 ;i<=pool.size() ;i++){
			sb.append("0x").append(pool.get(i-1)).append(",");
			if(i%4==0)
				sb.append("\n");
		}
		System.out.println(sb.toString());
	}

	public static String getCode(long idx) {
		return pool.get(Math.abs((int) idx % pool.size()));
	}

}
