package com.blackcrystalinfo.platform.util;

public class Constants extends ConfigurableConstants{

	static
	{
		init("sys.properties");
	}
	
	public static String SERVERIP = getProperty("server.ip", "");
	public static String SERVERPORT = getProperty("server.port", "");
	public static String SERVERPROTOCOL = getProperty("server.protocol", "");
	
	public static String REDIS_HOST = getProperty("redis.host", "192.168.2.14");
	
	public static String REDIS_PORT = getProperty("redis.port", "6379");
	
	public static String REDIS_PASS = getProperty("redis.pass", "");
	
	public static String WEBSOCKET_ADDR = getProperty("websocket.addr", "");
	
	public static String PIC_PATH = getProperty("pic.path","");
	
	public static int USRCFMEXPIRE = Integer.parseInt(getProperty("usr.cfm.expire",""));

	public static int CAPTCHA_EXPIRE = Integer.parseInt(getProperty("captcha.expire",""));
	
	public static int SERVER_PORT = Integer.valueOf(getProperty("server.port","8080"));
	
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
