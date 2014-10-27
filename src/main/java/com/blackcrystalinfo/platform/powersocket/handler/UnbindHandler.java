package com.blackcrystalinfo.platform.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.bind.UnbindResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

/**
 * 设备解绑功能(为手机方服务)
 * 
 * @author sophia
 */
public class UnbindHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UnbindHandler.class);

	public Object rpc(RpcRequest req) throws InternalException {
		UnbindResponse result = new UnbindResponse();
		result.setStatus(-1);
		result.setUrlOrigin(req.getUrlOrigin());

		String deviceId = HttpUtil.getPostValue(req.getParams(), "deviceId");
		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		logger.info("UnbindHandler begin  device:{}|userId:{}|cookie:{}", deviceId, userId, cookie);

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String id = jedis.hget("device:mac", deviceId);
			if (null == id) {
				result.setStatus(1);
				logger.info("Device does not exist. device:{}|userId:{}|cookie:{}|status:{}", deviceId, userId, cookie, result.getStatus());
				return result;
			}
			String shadow = jedis.hget("user:shadow", userId);
			if (shadow == null) {
				result.setStatus(2);
				logger.info("There isn't the man. device:{}|userId:{}|cookie:{}|status:{}", deviceId, userId, cookie, result.getStatus());
				return result;
			}
			if (!CookieUtil.validateMobileCookie(cookie, shadow, userId)) {
				result.setStatus(3);
				logger.info("The cookie fails you. device:{}|userId:{}|cookie:{}|status:{}", deviceId, userId, cookie, result.getStatus());
				return result;
			}

			long b1 = jedis.srem("bind:device:" + deviceId, userId);
			long b2 = jedis.srem("bind:user:" + userId, deviceId);
			if (b1 == 0 || b2 == 0) {
				result.setStatus(4); // 未绑定
				logger.info("The Device had unbinded with the user. device:{}|userId:{}|cookie:{}|status:{}", deviceId, userId, cookie, result.getStatus());
				return result;
			}

			// send message that is the users are related to the device to comet in format deviceId|userAId,userBId,userCId,etc..
			// Set<String> users = jedis.smembers("bind:device:" + deviceId);
			// String strUsers = StringUtils.join(users.toArray(), ",");
			// StringBuilder sb = new StringBuilder();
			// sb.append(deviceId).append("|").append(strUsers);
			// jedis.publish("PubDeviceUsers", sb.toString());

			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(userId).append("|").append("0");
			jedis.publish("PubDeviceUsers", sb.toString());

			result.setStatus(0);
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Bind out error. device:{}|userId:{}|cookie:{}|status:{}", deviceId, userId, cookie, result.getStatus(), e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: device:{}|userId:{}|cookie:{}|status:{}", deviceId, userId, cookie, result.getStatus());
		return result;
	}
}
