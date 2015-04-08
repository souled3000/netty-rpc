package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0005;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.HttpUtil;
@Path(path="/mobile/unbind")
public class UnbindApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UnbindApi.class);

	public Object rpc(JSONObject req) throws Exception {
		String mac = req.getString("mac");
		String cookie = req.getString("cookie");
		return deal(mac, cookie);
	}

	public Object rpc(RpcRequest req) throws Exception {
		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		return deal(mac, cookie);
	}
	
	
	private Object deal(String... args) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());

		String mac = args[0];
		String cookie = args[1];
		String family = CookieUtil.gotUserIdFromCookie(cookie);

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String id = j.hget("device:mactoid", mac);
			if (null == id) {
				r.put(status, C0003.toString());
				return r;
			}

			long b1 = j.zrem(family+"d", id);
			if (b1 == 0) {
				r.put(status, C0005.toString());
				return r;
			}
			
			//获取家庭成员
			Set<String> members = j.zrange(family+"u",0,-1);
			for(String o : members){
				StringBuilder sb = new StringBuilder();
				//将device与family下的所有用户切断关联
				sb.append(id).append("|").append(o).append("|").append("0");
				j.publish("PubDeviceUsers", sb.toString());
			}
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
