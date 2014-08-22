package com.blackcrystalinfo.platform.util;

public class Constants extends ConfigurableConstants{

	static
	{
		init("sys.properties");
	}
	
	public static String REDIS_HOST = getProperty("redis.host", "");
	
	public static String REDIS_PORT = getProperty("redis.port", "");
	
	public static String REDIS_PASS = getProperty("redis.pass", "");
	
	public static String WEBSOCKET_ADDR = getProperty("websocket.addr", "");
	public static String getProperty(String key, String defaultValue) {
		return p.getProperty(key, defaultValue);
	}
	public static void main(String[] args) {
		System.out.println(Constants.REDIS_HOST);
		System.out.println(Constants.REDIS_PORT);
		System.out.println(Constants.REDIS_PASS);
		System.out.println(Constants.WEBSOCKET_ADDR);
		System.out.println(Constants.getProperty("mem.threshold", ""));
		System.out.println(Constants.getProperty("cpu.threshold", ""));
		System.out.println(Constants.getProperty("handler.threshold", ""));
	}
}
