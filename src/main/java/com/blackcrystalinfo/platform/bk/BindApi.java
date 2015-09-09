package com.blackcrystalinfo.platform.bk;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0004;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

/**
 * 添加设备到家庭
 * 
 * @author juliana
 * 
 */
@Path(path = "/mobile/bind")
public class BindApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(BindApi.class);

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
		long l = System.currentTimeMillis();
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String mac = args[0];
		String cookie = args[1];
		String userId = CookieUtil.gotUserIdFromCookie(cookie);

		logger.info("begin BindHandler  fId:{}|mac:{}|cookie:{}", userId, mac, cookie);

		String deviceId = "";

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			deviceId = j.hget("device:mactoid", mac);
			if (null == deviceId) {
				r.put(status, C0003.toString());
				logger.info("There isn't this device. mac:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"));
				return r;
			}

			long b1 = j.zadd(userId + "d", (double) l, deviceId);// d:device;设备加入家庭;<fid+'d'>家庭设备组的key
			if (b1 == 0) {
				r.put(status, C0004.toString()); // 重复绑定
				logger.info("The device has been binded! mac:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"));
				return r;
			} else {
				j.zadd(userId + "u", (double) l, userId);// u:user;<fid+'u'>家庭用户组的key
				// 保存用户所属家庭
				j.zadd(userId, (double) l, userId);// 户主添加自己到家庭
				j.zadd(deviceId, (double) l, userId);
			}

			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(userId).append("|").append("1");
			j.publish("PubDeviceUsers", sb.toString());
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			logger.error("Bind in error mac:{}|cookie:{}|status:{}", mac, userId, cookie, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
