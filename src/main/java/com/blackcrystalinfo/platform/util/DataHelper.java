package com.blackcrystalinfo.platform.util;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

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

	public static void main2(String[] args) {
		Jedis j = DataHelper.getJedis();
		j.set("sophia".getBytes(), "lchj".getBytes());
		System.out.println(j.get("sophia"));
		String userId = "";
		j.hset((userId + "f").getBytes(), "ct".getBytes(), NumberByte.long2Byte(System.currentTimeMillis()));
		byte[] b = j.hget("f".getBytes(), "ct".getBytes());
		long l = NumberByte.byte2Long(b);
		System.out.println(l);
		Date d = new Date();
		d.setTime(l);
		System.out.println(d.toLocaleString());
		DataHelper.returnJedis(j);

		Jedis j2 = null;
		try {
			j2 = DataHelper.getJedis();
			throw new Exception();
		} catch (Exception e) {
			//DataHelper.returnBrokenJedis(j2);
		} finally {
			DataHelper.returnJedis(j2);
		}
	}
	
	public static void main(String[] args) {
		Jedis j = DataHelper.getJedis();
		Long l = System.currentTimeMillis();
		byte[] p = j.get("a".getBytes());
		
		Map m = (Map)j.hgetAll("user:group:64");
		for(Map.Entry<?,?> e : (Set<Map.Entry<?, ?>>)m.entrySet()){
			System.out.println(e.getKey()+"---"+e.getValue());
			List<Map> v = (List<Map>)JSON.parse((String)e.getValue());
			for(Map vv: v){
				for(Map.Entry<?,?> vvv :(Set<Map.Entry<?,?>>)vv.entrySet()){
					System.out.println(vvv.getKey()+ "    "+ vvv.getValue());
				}
			}
			System.out.println("-------------------------------");
		}
		System.out.println(System.currentTimeMillis()-l);
		DataHelper.returnJedis(j);
		
		System.out.println("-------------------------------");
		String d = "FCFFFFFFFFFFFFFF";
		byte[] ddd = new byte[8];
		int f = 0;
		for(int w = 2;w<=16;w+=2){
			String ssss = d.substring(w-2, w);
			System.out.println();
			ddd[(w-2)/2]=(byte)Integer.parseInt(ssss,16);
		}
		System.out.println(ddd[0]);
		System.out.println(NumberByte.byte2LongLittleEndian(ddd));
		System.out.println(NumberByte.byte2LongLittleEndian(ByteUtil.fromHex(d)));
		System.out.println();
	}
}
