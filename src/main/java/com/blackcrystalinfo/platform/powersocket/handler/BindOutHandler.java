package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.bind.BindOutResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class BindOutHandler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(BindOutHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		BindOutResponse result = new BindOutResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		logger.info("BindOutHandler begin  mac:{}|userId:{}|cookie:{}", mac, userId, cookie);
		String deviceId = "";

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String id = jedis.hget("device:mactoid", mac);
			if (null == id) {
				result.setStatus(2);
				logger.info("Mac does not exist. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
				return result;
			}
			try {
				if (!CookieUtil.verifyDeviceKey(mac, cookie, id)) {
					logger.info("mac not matched cookie mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
					return result;
				}
				deviceId = CookieUtil.extractDeviceId(cookie);
			} catch (Exception e) {
				logger.error("Cookie decode error.mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus(),e);
				return result;
			}
			long b1 = jedis.srem("bind:device:" + deviceId, userId);
			long b2 = jedis.srem("bind:user:" + userId, deviceId);
			if (b1 == 0 || b2 == 0) {
				result.setStatus(12); // 未绑定
				logger.info("User device not binded! mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
				return result;
			}
			
			// send message that is the users are related to the device to comet in format deviceId|userAId,userBId,userCId,etc..
			Set<String> users = jedis.smembers("bind:device:" + deviceId);
			String strUsers = StringUtils.join(users.toArray(), ",");
			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(strUsers);
			jedis.publish("PubDeviceUsers", sb.toString());

			result.setStatus(0);
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Bind out error. mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus(),e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: mac:{}|userId:{}|cookie:{}|status:{}", mac, userId, cookie,result.getStatus());
		return result;
	}
}
