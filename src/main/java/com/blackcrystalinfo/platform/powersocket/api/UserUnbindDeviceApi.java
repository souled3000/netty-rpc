package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0005;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.Utils;

@Path(path="/mobile/unbind")
public class UserUnbindDeviceApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserUnbindDeviceApi.class);

	public Object rpc(JSONObject req) throws Exception {
		String mac = req.getString("mac");
		String cookie = req.getString("cookie");
		return deal(mac, cookie);
	}

	public Object rpc(RpcRequest req) throws Exception {
		String mac = req.getParameter( "mac");
		String cookie = req.getParameter( "cookie");
		return deal(mac, cookie);
	}
	
	
	private Object deal(String... args) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());

		String mac = args[0];
		String userId = CookieUtil.gotUserIdFromCookie(args[1]);

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String deviceId = j.hget("device:mactoid", mac);
			if (null == deviceId) {
				r.put(status, C0003.toString());
				return r;
			}
			String mac2 = j.hget("device:mac2", deviceId);
			String owner = j.hget("device:owner", deviceId);
			if(owner==null||!StringUtils.equals(owner, userId)){
				r.put(status, C0005.toString());
				return r;
			}
			
			j.hdel("device:owner", deviceId);
			j.srem("u:"+userId+":devices", deviceId);
			
			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(userId).append("|").append("0");
			j.publish("PubDeviceUsers", sb.toString());
			j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(userId+"|",8, mac2, ""));
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("",e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status,SUCCESS.toString());
		return r;
	}
}
