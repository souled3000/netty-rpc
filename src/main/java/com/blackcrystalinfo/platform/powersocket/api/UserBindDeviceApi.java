package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0004;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

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
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

/**
 * 添加设备到家庭
 * 
 * @author juliana
 * 
 */
@Controller("/mobile/bind")
public class UserBindDeviceApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserBindDeviceApi.class);

	@Autowired
	private IDeviceDao deviceDao;

	public Object rpc(JSONObject req) throws Exception {
		String mac = req.getString("mac");
		String cookie = req.getString("cookie");
		return deal(mac, cookie);
	}

	public Object rpc(RpcRequest req) throws Exception {
		String mac = req.getParameter("mac");
		String cookie = req.getParameter("cookie");
		return deal(mac, cookie);
	}

	private Object deal(String... args) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String mac = args[0];
		String userId = CookieUtil.gotUserIdFromCookie(args[1]);

		logger.info("begin BindHandler  user:{}|mac:{}", userId, mac);

		String deviceId = "";

		// 手机端需要mac区分绑定解绑的是哪个设备，这里给返回。是不是很奇葩。。。
		r.put("mac", mac);

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			if (!deviceDao.exists(mac)) {
				r.put(status, C0003.toString());
				logger.info("There isn't this device. mac:{}|user:{}|status:{}", mac, userId, r.get("status"));
				return r;
			}

			deviceId = String.valueOf(deviceDao.getIdByMac(mac));
			String owner = j.hget("device:owner", deviceId);
			if (owner != null) {
				r.put(status, C0004.toString());
				logger.info("The device has been binded! mac:{}|user:{}|status:{}", mac, userId, r.get("status"));
				return r;
			} else {
				j.hset("device:owner", deviceId, userId);
				j.hset("device:bindtime", deviceId, String.valueOf(System.currentTimeMillis()));
				j.sadd("u:" + userId + ":devices", deviceId);
			}

			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(userId).append("|").append("1");
			j.publish("PubDeviceUsers", sb.toString());
		} catch (Exception e) {
			logger.error("Bind in error mac:{}|user:{}|status:{}", mac, userId, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
