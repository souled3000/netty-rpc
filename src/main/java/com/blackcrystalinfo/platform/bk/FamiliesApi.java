package com.blackcrystalinfo.platform.bk;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Path(path = "/mobile/families")
public class FamiliesApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(FamiliesApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String cookie = req.getParameter("cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			Set<String> familySet = j.zrange(userId, 0, -1);
			r.put("families", familySet);
			r.put(status, SUCCESS);
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		return r;
	}
}
