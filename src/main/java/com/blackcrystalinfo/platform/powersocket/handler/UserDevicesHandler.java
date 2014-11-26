package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.device.DeviceData;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserDevicesResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class UserDevicesHandler extends HandlerAdapter {

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
		
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String shadow = jedis.hget("user:shadow", userId);
			
			if(!CookieUtil.validateMobileCookie(cookie, shadow, userId)){
				resp.setStatus(7);
				logger.info("user:shadow don't match user's ID. userId:{}|cookie:{}|status:{}",userId,cookie,resp.getStatus());
				return resp;
			}
			
			Set<String> devIds = jedis.smembers("bind:user:" + userId);

			List<DeviceData> bindedDevices = new ArrayList<DeviceData>();
			for (String id : devIds) {
				DeviceData devData = new DeviceData();
				String mac = jedis.hget("device:mac", id);
				String name = jedis.hget("device:name", id);
				String pwd = jedis.hget("device:pwd:"+userId, id);
				String dv = jedis.hget("device:dv", id);
				
				name = (null == name ? "default" : name);

				devData.setDeviceId(id);
				devData.setMac(mac);
				devData.setDeviceName(name);
				devData.setPwd(pwd);
				devData.setDeviceType(dv);
				bindedDevices.add(devData);
			}

			resp.setStatus(0);
			resp.setBindedDevices(bindedDevices);
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Get user's device error. userId:{}|cookie:{}|status:{}",userId,cookie,resp.getStatus(),e);
			return resp;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: userId:{}|cookie:{}|status:{}",userId,cookie,resp.getStatus());
		return resp;
	}

}
