package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.dao.IDeviceDao;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.DataHelper;

/**
 * 设备强制解绑
 * 
 * @author j
 * 
 */
@Controller("/api/device/unbindbydv")
public class UnbindByDv extends HandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(UnbindByDv.class);

	@Autowired
	private IDeviceDao deviceDao;

	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		return deal(mac);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter("mac");
		return deal(mac);
	}

	public Object deal(String... args) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String mac = args[0];
		logger.info("Unbind user by device begin, mac:{} ", mac);

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			Long id = deviceDao.getIdByMac(mac);
			if (id == null) {
				r.put("status", 1);
				logger.error("Device not exist, mac:{}", mac);
				return r;
			}

			String deviceId = String.valueOf(id);
			String owner = j.hget("device:owner", deviceId);
			if (owner != null) {
				j.hdel("device:owner", deviceId);
				j.srem("u:" + owner + ":devices", deviceId);

				StringBuilder sb = new StringBuilder();
				sb.append(deviceId).append("|").append(owner).append("|")
						.append("0");
				j.publish("PubDeviceUsers", sb.toString());
			} else {
				logger.info("Device is not binded~~~");
			}
		} catch (Exception e) {
			logger.error("Device Unbind error e = ", e);
			r.put("status", -1);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put("status", 0);
		return r;
	}

}