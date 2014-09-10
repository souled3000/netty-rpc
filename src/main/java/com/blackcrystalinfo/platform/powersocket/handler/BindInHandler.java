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
import com.blackcrystalinfo.platform.powersocket.dao.pojo.bind.BindInResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class BindInHandler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(BindInHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		BindInResponse result = new BindInResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");

		logger.info("BindInHandler  mac:{}|userId:{}|cookie:{}", mac, userId, cookie);

		String deviceId = "";

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			
			String email = jedis.hget("user:email", userId);
			if(null == email){
				result.setStatus(3);
				logger.info("There is not this user.");
				return result;
			}
			
			
			deviceId = jedis.hget("device:mactoid", mac);
			if (null == deviceId) {
				result.setStatus(2);
				logger.info("Mac does not exist.mac:{}", mac);
				return result;
			}
			try {
				if (!CookieUtil.verifyDeviceKey(mac, cookie, deviceId)) {
					result.setStatus(1);
					result.setStatusMsg("mac do not match cookie");
					return result;
				}
			} catch (Exception e) {
				logger.error("Cookie decode error.");
				result.setStatusMsg("Cookie decode error.");
				return result;
			}
			long b1 = jedis.sadd("bind:device:" + deviceId, userId);
			long b2 = jedis.sadd("bind:user:" + userId, deviceId);
			if (b1 == 0 || b2 == 0) {
				result.setStatus(11); // 重复绑定
				logger.info("User device has binded! deviceId:{}|userId:{}|mac:{}|cookie:{}", deviceId, userId, mac, cookie);
				return result;
			}

			result.setStatus(0);

			// send message that is the users are related to the device to comet in format deviceId|userAId,userBId,userCId,etc..
			Set<String> users = jedis.smembers("bind:device" + deviceId);
			String strUsers = StringUtils.join(users.toArray(), ",");
			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(strUsers);
			jedis.publish("PubDeviceUsers", sb.toString());

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Bind in error", e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result.getStatus());
		return result;
	}
}
