package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.bind.BindInResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class BindInHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(BindInHandler.class);

	@Override
	public Object rpc(JSONObject req) throws InternalException {

		BindInResponse result = new BindInResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
//		result.setUrlOrigin(req.getUrlOrigin());

		String mac = req.getString("mac");
		String userId = req.getString("userId");
		String cookie = req.getString("cookie");
		
		logger.info("BindInHandler  mac:{}|userId:{}|cookie:{}", mac, userId, cookie);

		String deviceId = "";

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			
			String email = jedis.hget("user:email", userId);
			if(null == email){
				result.setStatus(3);
				logger.info("There is not this user. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
				return result;
			}
			
			
			deviceId = jedis.hget("device:mactoid", mac);
			if (null == deviceId) {
				result.setStatus(2);
				logger.info("Mac does not exist. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
				return result;
			}
			try {
				if (!CookieUtil.verifyDeviceKey(mac, cookie, deviceId)) {
					result.setStatus(1);
					logger.info("mac do not match cookie mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
					return result;
				}
			} catch (Exception e) {
				logger.error("Cookie decode error. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus(),e);
				return result;
			}
			long b1 = jedis.sadd("bind:device:" + deviceId, userId);
			long b2 = jedis.sadd("bind:user:" + userId, deviceId);
			if (b1 == 0 || b2 == 0) {
				result.setStatus(11); // 重复绑定
				logger.info("User device has binded! mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
				return result;
			}

			result.setStatus(0);
			// send message that is the users are related to the device to comet in format deviceId|userAId,userBId,userCId,etc..
//			Set<String> users = jedis.smembers("bind:device:" + deviceId);
//			String strUsers = StringUtils.join(users.toArray(), ",");
//			StringBuilder sb = new StringBuilder();
//			sb.append(deviceId).append("|").append(strUsers);
//			jedis.publish("PubDeviceUsers", sb.toString());

			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(userId).append("|").append("1");
			jedis.publish("PubDeviceUsers", sb.toString());
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Bind in error mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus(),e);
			return result;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
		return result;
	}
	
}
