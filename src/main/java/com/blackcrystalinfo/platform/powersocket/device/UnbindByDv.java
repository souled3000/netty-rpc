package com.blackcrystalinfo.platform.powersocket.device;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.InternalException;

/**
 * 设备强制解绑
 * 
 * @author j
 * 
 */
@Controller("/api/device/unbindbydv")
public class UnbindByDv extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UnbindByDv.class);

	public Object rpc(JSONObject req) throws InternalException {
		String id = req.getString("id");
		return deal(id);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String id = req.getParameter("id");
		return deal(id);
	}

	public Object deal(String... args) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String id = args[0];
		logger.info("Unbind user by device begin, id:{} ", id);

		Jedis j = null;
		try {
			j = JedisHelper.getJedis();

			String deviceId = String.valueOf(id);
			String owner = j.hget("device:owner", deviceId);
			if (owner != null) {
				j.hdel("device:owner", deviceId);
				j.srem("u:" + owner + ":devices", deviceId);

				StringBuilder sb = new StringBuilder();
				sb.append(deviceId).append("|").append(owner).append("|").append("0");
				j.publish("PubDeviceUsers", sb.toString());
			} else {
				logger.info("Device is not binded~~~");
				r.put("status", 1);
				return r;
			}
		} catch (Exception e) {
			logger.error("Device Unbind error e = ", e);
			r.put("status", -1);
			return r;
		} finally {
			JedisHelper.returnJedis(j);
		}

		r.put("status", 0);
		return r;
	}

}