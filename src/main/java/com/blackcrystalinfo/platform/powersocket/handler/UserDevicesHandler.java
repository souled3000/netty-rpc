package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.device.DeviceData;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserDevicesResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class UserDevicesHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UserDevicesHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		UserDevicesResponse resp = new UserDevicesResponse();
		resp.setStatus(-1);
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		logger.info("UserDevices begin userId:{}|cookie:{}",userId,cookie);
		
		String[] infos = null;
		try {
			infos = CookieUtil.decode(cookie);

			String tmpUserId = infos[0];
			if (!userId.equals(tmpUserId)) {
				resp.setStatus(1);
				logger.info("userId do not match with cookie. userId:{}|cookie:{}|status:{}",userId,cookie,resp.getStatus());
				return resp;
			}
		} catch (Exception e) {
			logger.error("Cookie decode error. userId:{}|cookie:{}|status:{}",userId,cookie,resp.getStatus(),e);
			return resp;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			Set<String> devIds = jedis.smembers("bind:user:" + userId);

			List<DeviceData> bindedDevices = new ArrayList<DeviceData>();
			for (String id : devIds) {
				DeviceData devData = new DeviceData();
				String mac = jedis.hget("device:mac", id);
				String name = jedis.hget("device:name:"+userId, id);
				String pwd = jedis.hget("device:pwd:"+userId, id);
				// TODO: just for test, delete later
				name = (null == name ? "default" : name);

				devData.setDeviceId(id);
				devData.setMac(mac);
				devData.setDeviceName(name);
				devData.setPwd(pwd);
				bindedDevices.add(devData);
			}

			resp.setStatus(0);
			resp.setBindedDevices(bindedDevices);
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Get user's device error. userId:{}|cookie:{}|status:{}",userId,cookie,resp.getStatus(),e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: userId:{}|cookie:{}|status:{}",userId,cookie,resp.getStatus());
		return resp;
	}

}
