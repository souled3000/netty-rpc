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
		logger.info("[mac:{}][userId:{}][cookie:{}]", mac, userId, cookie);
		String deviceId = "";
		try {
			if (!CookieUtil.verifyDeviceKey(mac, cookie)) {
				logger.error("mac={}&cookie={}, not matched!!!", mac, cookie);
				result.setStatusMsg("mac not matched cookie");
				return result;
			}
			deviceId = CookieUtil.extractDeviceId(cookie);
		} catch (Exception e) {
			logger.error("Cookie decode error.", e);
			result.setStatusMsg("Cookie decode error.");
			return result;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			if ((jedis.srem("bind:device:" + deviceId, userId) == 0) || (jedis.srem("bind:user:" + userId, deviceId) == 0)) {
				result.setStatus(12); // 未绑定
				result.setStatusMsg("User device not binded!");
				return result;
			}
			
			
			//send message that is the users are related to the device to comet in format deviceId|userAId,userBId,userCId,etc..
			Set<String> users = jedis.smembers("bind:device"+deviceId);
			String strUsers = StringUtils.join(users.toArray(), ",");
			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(strUsers);
			jedis.publish("PubDeviceUsers", sb.toString());
			
			result.setStatus(0);
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Bind out error", e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}
}
