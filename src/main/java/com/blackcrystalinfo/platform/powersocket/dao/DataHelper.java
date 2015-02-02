package com.blackcrystalinfo.platform.powersocket.dao;

import java.util.Date;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.NumberByte;

public class DataHelper {
	private static final JedisPool pool;
	private static final int ONE_SECOND = 1000;

	static {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(1000);
		poolConfig.setMaxWaitMillis(ONE_SECOND);

		String host = Constants.REDIS_HOST;
		int port = Integer.parseInt(Constants.REDIS_PORT);
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
		Jedis j = DataHelper.getJedis();
		j.set("sophia".getBytes(), "lchj".getBytes());
		System.out.println(j.get("sophia"));
		String userId="";
		j.hset((userId+"f").getBytes(), "ct".getBytes(), NumberByte.long2Byte(System.currentTimeMillis()));
		byte[] b = j.hget("f".getBytes(),"ct".getBytes());
		long l = NumberByte.getLong(b);
		System.out.println(l);
		Date d = new Date();
		d.setTime(l);
		System.out.println(d.toLocaleString());
		DataHelper.returnJedis(j);
	}
}
