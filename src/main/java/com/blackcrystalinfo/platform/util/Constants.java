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

	public static int REGAGAIN_TIMES_NOTIC = Integer.valueOf(getProperty("regagain.times.notic", "3"));
	public static int REGAGAIN_TIMES_MAX = Integer.valueOf(getProperty("regagain.times.max", "5"));
	public static int REGAGAIN_EXPIRE = Integer.valueOf(getProperty("regagain.expire", "86400"));

	public static int FAILED_LOGIN_TIMES_MAX = Integer.valueOf(getProperty("failed.login.times.max", "3"));
	public static int FAILED_LOGIN_EXPIRE = Integer.valueOf(getProperty("failed.login.expire", "86400"));

	/**
	 * 邮箱激活连接的有效期
	 */
	public static int MAIL_ACTIVE_EXPIRE = Integer.valueOf(getProperty("mail.active.expire", "86400"));

	/**
	 * 用户修改密码限制
	 */
	public static int PASSWD_CHANGED_TIMES_MAX = Integer.valueOf(getProperty("passwd.changed.times.max", "2"));
	public static int PASSWD_CHANGED_EXPIRE = Integer.valueOf(getProperty("passwd.changed.expire", "86400"));

	/**
	 * 数据库密码密文保存
	 */
	public static String DB_PWD_RAWKEY = getProperty("db.pwd.rawkey", "blackcrystal");

	/**
	 * 设备注册校验（需求原文->设备授权管理）
	 */
	public static boolean DEV_REG_VALID = Boolean.valueOf(getProperty("dev.reg.valid", "true"));
	public static String DEV_REG_LIC_PATH = getProperty("dev.reg.lic.path", "");

	/**
	 * 是否使用SSL方式发送邮件
	 */
	public static boolean MAIL_SENDER_SSL = Boolean.valueOf(getProperty("mail.sender.ssl", "false"));

	/**
	 * 发送短信中间件地址
	 */
	public static String SMS_SENDER_URL = getProperty("sms.sender.url", "http://127.0.0.1/sms");

	/**
	 * 用户加入家庭超时时间
	 */
	public static int USER_INVITATION_CFM_EXPIRE = Integer.valueOf(getProperty("user.invitation.cfm.expire", "300"));

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
