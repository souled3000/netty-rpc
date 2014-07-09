package com.github.com.nettyrpc.util;

public class Constants extends ConfigurableConstants{

	static
	{
		init("sys.properties");
	}
	
	public static String REDIS_HOST = getProperty("redis.host", "localhost");
	
	public static String REDIS_PORT = getProperty("redis.port", "6379");
	
	public static String REDIS_PASS = getProperty("redis.pass", null);
	
	public static String WEBSOCKET_ADDR = getProperty("websocket.addr", "ws://127.0.0.1/ws");
	
	public static void main(String[] args) {
		System.out.println(Constants.REDIS_HOST);
		System.out.println(Constants.REDIS_PORT);
		System.out.println(Constants.REDIS_PASS);
		System.out.println(Constants.WEBSOCKET_ADDR);
		
		
	}
}
