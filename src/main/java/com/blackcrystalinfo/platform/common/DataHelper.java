package com.blackcrystalinfo.platform.common;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DataHelper {
	private static final JedisPool pool;
	private static final int ONE_SECOND = 10000;

	private static final int DB_INDEX = 0;
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

	public static void main(String[] args) throws Exception{
//		Jedis j = DataHelper.getJedis();
//		Set<String> keys =j.keys("B0029*");
//		for(String key:keys){
//			j.del(key);
//		}
//		DataHelper.returnJedis(j);
		
	}
}
