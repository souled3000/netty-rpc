package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.server.CometScanner;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.InternalException;

@Controller("/mobile/cometadr")
public class WsAdrApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(WsAdrApi.class);

	public Object rpc(RpcRequest req) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		Jedis jedis = null;
		try {
			jedis = JedisHelper.getJedis();

			String userId = req.getUserId();

			String heartBeat = CookieUtil.EXPIRE_SEC;
			String proxyKey = CookieUtil.genWsKey(userId, String.valueOf(System.currentTimeMillis() / 1000), CookieUtil.EXPIRE_SEC);
			String proxyAdr = String.format("ws://%s/ws", CometScanner.take());
			r.put("proxyAddr", proxyAdr);
			r.put("heartBeat", heartBeat);
			r.put("proxyKey", proxyKey);
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("", e);
			return r;
		} finally {
			JedisHelper.returnJedis(jedis);
		}
		return r;
	}
}