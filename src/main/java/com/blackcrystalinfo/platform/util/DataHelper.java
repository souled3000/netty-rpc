package com.blackcrystalinfo.platform.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DataHelper {
	private static final JedisPool pool;
	private static final int ONE_SECOND = 10000;

	private static final int DB_INDEX = 1;
	static {
		JedisPoolConfig cfg = new JedisPoolConfig();
		cfg.setMaxIdle(5);
		cfg.setMaxWaitMillis(ONE_SECOND);
		cfg.setTestOnBorrow(true);
		cfg.setMaxTotal(100000);
		String host = Constants.REDIS_HOST;
		int port = Integer.parseInt(Constants.REDIS_PORT);
		String password = null;

		pool = new JedisPool(cfg, host, port, ONE_SECOND, password);
	}

	public static Jedis getJedis() {
		Jedis jedis = pool.getResource();
		jedis.select(DB_INDEX);
		return jedis;
	}

	public static void returnJedis(Jedis res) {
		if (res != null && res.isConnected()) {
			pool.returnResourceObject(res);
		}
	}

	public static void returnBrokenJedis(Jedis res) {
		if (res != null && res.isConnected()) {
			pool.returnResourceObject(res);
		}
	}

}
