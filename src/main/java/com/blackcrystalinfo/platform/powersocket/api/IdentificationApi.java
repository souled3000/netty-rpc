package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0001;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0002;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.HttpUtil;
@Path(path="/mobile")
public class IdentificationApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(HandlerAdapter.class);

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String userId = CookieUtil.gotUserIdFromCookie(cookie);

			String email = j.hget("user:email", userId);
			if (null == email) {
				r.put(status, C0001);
				logger.info("failed validating user {}", r.get(status));
				return r;
			}

			String shadow = j.hget("user:shadow", userId);
			if (!CookieUtil.validateMobileCookie(cookie, shadow)) {
				r.put(status, C0002);
				logger.info("failed validating user {}", r.get(status));
				return r;
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, SUCCESS.toString());
		return r;
	}
}
