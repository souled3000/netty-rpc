package com.blackcrystalinfo.platform.util.sms;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.common.Constants;

public class SMSSender {

	private static final Logger logger = LoggerFactory.getLogger(SMSSender.class);

	public static boolean send(String phone, String content) {

		logger.info("send SMS: phone={}, content={}", phone, content);
		try {
			String type = "submit";

			StringBuilder sb = new StringBuilder();
			sb.append("type=").append(type);
			sb.append("&phones=").append(phone);
			sb.append("&content=").append(content);

			String ret = SimpleHttpClientUtil.beg(Constants.SMS_SENDER_URL, sb.toString());

			logger.info("send ret={}", ret);

			return true;
		} catch (IOException e) {
			logger.error("send short message error!!!", e);
		}

		return false;
	}

	public static void main(String[] args) {
		// SMSSender.send("18612455087", "hello_cj");
		System.out.println(1 - 0.9);
		char x = '李';
		System.out.println(x);
		System.out.println((int) x);
		int val = 3;
		System.out.println(isPowerOfTwo(val));
		System.out.println(-val);

		AtomicInteger n = new AtomicInteger(0);
		long l = System.currentTimeMillis();
		for ( int i = 0 ; i <=Integer.MAX_VALUE;i++){
//			int a = n.getAndIncrement()&100-1;
		}
		l = System.currentTimeMillis()-l;
		System.out.println(l/1000/60/60);
		System.out.println(0&100-2);

Properties props=System.getProperties(); //获得系统属性集  
String osName = props.getProperty("os.name"); //操作系统名称  
String osArch = props.getProperty("os.arch"); //操作系统构架  
String osVersion = props.getProperty("os.version"); //操作系统版本  
		System.out.println(System.getProperties().getProperty("os.name"));
		System.out.println(osArch);
		System.out.println(osVersion);
	}

	public static boolean isPowerOfTwo(int val) {
		return (val & -val) == val;
	}
}
