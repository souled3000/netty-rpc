package com.github.com.nettyrpc.powersocket.dao;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DataHelper {
	private static final JedisPool pool;
	private static final int ONE_SECOND = 1000;

	static {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(1000);		
		poolConfig.setMaxWaitMillis(ONE_SECOND);
//		String host = "193.168.1.14";
		String host = "127.0.0.1";
		int port = 6379;
		String password = null;

		pool = new JedisPool(poolConfig, host, port, ONE_SECOND, password);
	}

	public static Jedis getJedis() {
		return pool.getResource();
	}
	
	public static void returnJedis(Jedis res) {
		if (res != null) {
			pool.returnResource(res);
		}
	}
	
	public static void returnBrokenJedis(Jedis res) {
		if (res != null) {
			pool.returnBrokenResource(res);
		}
	}

	public static void main(String[] args) {
		System.out.println(DataHelper.getJedis());
	}
}
