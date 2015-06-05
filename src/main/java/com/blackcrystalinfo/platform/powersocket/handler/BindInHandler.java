package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;


@Path(path="/api/bind/in")
public class BindInHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(BindInHandler.class);

	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		String userId = req.getString("userId");
		String cookie = req.getString("cookie");
		return deal(mac, userId, cookie);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter( "mac");
		String userId = req.getParameter( "userId");
		String cookie = req.getParameter( "cookie");
		return deal(mac, userId, cookie);
	}

	private Object deal(String... args) throws InternalException {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put("status",-1);

		String mac = args[0];
		String userId = args[1];
		String cookie = args[2];

		logger.info("BindInHandler  mac:{}|userId:{}|cookie:{}", mac, userId, cookie);

		String deviceId = "";

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String email = jedis.hget("user:email", userId);
			if (null == email) {
				r.put("status",3);
				logger.info("There is not this user. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"));
				return r;
			}

			deviceId = jedis.hget("device:mactoid", mac);
			if (null == deviceId) {
				r.put("status",2);
				logger.info("Mac does not exist. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"));
				return r;
			}
			try {
				if (!CookieUtil.verifyDeviceKey(mac, cookie, deviceId)) {
					r.put("status",1);
					logger.info("mac do not match cookie mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"));
					return r;
				}
			} catch (Exception e) {
				logger.error("Cookie decode error. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"), e);
				return r;
			}
			long b1 = jedis.sadd("bind:device:" + deviceId, userId);
			long b2 = jedis.sadd("bind:user:" + userId, deviceId);
			if (b1 == 0 || b2 == 0) {
				r.put("status",11); // 重复绑定
				logger.info("User device has binded! mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"));
				return r;
			}

			r.put("status",0);
			// send message that is the users are related to the device to comet in format deviceId|userAId,userBId,userCId,etc..
			// Set<String> users = jedis.smembers("bind:device:" + deviceId);
			// String strUsers = StringUtils.join(users.toArray(), ",");
			// StringBuilder sb = new StringBuilder();
			// sb.append(deviceId).append("|").append(strUsers);
			// jedis.publish("PubDeviceUsers", sb.toString());

			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(userId).append("|").append("1");
			jedis.publish("PubDeviceUsers", sb.toString());
		} catch (Exception e) {
			//DataHelper.returnBrokenJedis(jedis);
			logger.error("Bind in error mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"));
		return r;

	}
	
}
