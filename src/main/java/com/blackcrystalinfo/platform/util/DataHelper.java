package com.blackcrystalinfo.platform.util;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DataHelper {
	private static final JedisPool pool;
	private static final int ONE_SECOND = 10000;

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
		return pool.getResource();
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
	public static void main(String[] args) throws Exception {
		byte[] array1 =null;
//		array1[0]=1;
		byte[] array2 =new byte[3];
		array2[0]=1;
		byte[] a = ArrayUtils.addAll(array1, array2);
		
		
		
		System.out.println(Arrays.toString(a));
//		f1();
	}

	private static void f1() throws Exception {
		Jedis j = DataHelper.getJedis();
		j.hset("hhh".getBytes(), NumberByte.long2Byte(1), "aaaa".getBytes());
		DataHelper.returnJedis(j);
	}
}
