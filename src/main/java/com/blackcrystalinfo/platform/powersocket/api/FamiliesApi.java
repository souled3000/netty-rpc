package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

import redis.clients.jedis.Jedis;

@Path(path="/mobile/families")
public class FamiliesApi extends HandlerAdapter {
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String userId = CookieUtil.gotUserIdFromCookie(req.getParameter( "cookie"));
		
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String fId = j.hget("user:family", userId);
			Set<String> familySet = j.smembers("family:"+fId);
			r.put("fId", fId);
			r.put("families", familySet);
			r.put(status,SUCCESS.toString());
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		return r;
	}
	public static void main(String[] args) {
		Set<Integer> s = new HashSet<Integer>();
		s.add(1);
		s.add(16);
		s.add(32);
		Map<String, Set<Integer>> m = new HashMap<String, Set<Integer>>();
		m.put("families", s);
		System.out.println(JSON.toJSON(s));
		System.out.println(JSON.toJSON(m));
	}
}
