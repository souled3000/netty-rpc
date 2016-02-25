package com.blackcrystalinfo.platform.common;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

public class DataHelper {
	private static final JedisPool pool;
	private static final int ONE_SECOND = 10000;

	private static final int DB_INDEX = 0;

	static {
		JedisPoolConfig cfg = new JedisPoolConfig();
		cfg.setMaxIdle(5);
		cfg.setMaxWaitMillis(ONE_SECOND);
		cfg.setTestOnBorrow(true);
		cfg.setMaxTotal(Constants.RedisMaxTotal);
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

	private static Connection con = null;

	public static Connection getHCon() {
		a: if (con == null || con.isClosed()) {
			// System.setProperty("hadoop.home.dir", "C:\\hadoop-common-2.2.0-bin-master\\");
			Configuration config = HBaseConfiguration.create();
			try {
				con = ConnectionFactory.createConnection(config);
			} catch (IOException e) {
				try {
					Thread.sleep(3000);
					break a;
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		return con;
	}

	public static void main(String[] args) throws Exception {
		f2();
	}

	private static void f1() throws Exception {
		Jedis j = DataHelper.getJedis();
		Set<String> keys = j.keys("B0029*");
		for (String key : keys) {
			j.del(key);
		}
		DataHelper.returnJedis(j);
	}

	private static void f2() throws Exception {
		ExecutorService es = Executors.newCachedThreadPool();
		for (int i = 0; i < 100; i++)
			es.submit(new Callable<Object>() {
				public Object call() throws Exception {
					Jedis j = DataHelper.getJedis();
					Transaction t =j.multi();
					for (int n = 0; n<1000;n++){
						t.incr("K");
					}
					t.exec();
					DataHelper.returnJedis(j);
					return null;
				}
			});
		es.shutdown();
		es.awaitTermination(Long.MAX_VALUE	, TimeUnit.DAYS);
		Jedis j = DataHelper.getJedis();
		System.out.println(j.get("K"));
		DataHelper.returnJedis(j);
	}
}
