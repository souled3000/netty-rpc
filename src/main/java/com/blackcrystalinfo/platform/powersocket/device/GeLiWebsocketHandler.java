package com.blackcrystalinfo.platform.powersocket.device;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.powersocket.mobile.WsAdrApi;
import com.blackcrystalinfo.platform.server.CometScanner;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.InternalException;

@Controller("/api/geli/getUrl")
public class GeLiWebsocketHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(WsAdrApi.class);

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
