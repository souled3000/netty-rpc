package com.blackcrystalinfo.platform.powersocket.api;

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
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Path(path="/mobile/cometadr")
public class CometAdrApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(CometAdrApi.class);

	public Object rpc(RpcRequest req) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String cookie = req.getParameter( "cookie");

		logger.info("WebsocketInfoHandler begin cookie:{}", cookie);

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String userId = CookieUtil.gotUserIdFromCookie(cookie);

			String heartBeat = CookieUtil.EXPIRE_SEC;
			String proxyKey = CookieUtil.generateKey(userId, String.valueOf(System.currentTimeMillis() / 1000), CookieUtil.EXPIRE_SEC);

			r.put("proxyAddr", CometScanner.take());
			r.put("heartBeat", heartBeat);
			r.put("proxyKey", proxyKey);
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("System error occurs", e);
			//DataHelper.returnBrokenJedis(jedis);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		return r;
	}
}
