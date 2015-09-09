package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.api.CometAdrApi;
import com.blackcrystalinfo.platform.util.CometScanner;

@Path(path = "/api/geli/getUrl")
public class GeLiWebsocketHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(CometAdrApi.class);

	public Object rpc(RpcRequest req) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		// 1. 校验cookie信息
		try {
			// resp.setProxyAddr(CometScannerV2.take());
			r.put("proxyAddr", CometScanner.take());
			r.put("status", 0);
		} catch (Exception e) {
			logger.error("System error occurs", e);
			r.put("status", -1);
			return r;
		}
		logger.info("response: status:{}|proxyAddr:{}", r.get("status"), r.get("proxyAddr"));
		return r;
	}
}
