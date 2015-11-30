package com.blackcrystalinfo.platform.powersocket.device;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.powersocket.mobile.WsAdrApi;
import com.blackcrystalinfo.platform.server.CometScanner;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.InternalException;

public class GeliIPRefreshHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(WsAdrApi.class);

	public Object rpc(RpcRequest req) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		try {
			CometScanner.refresh();
			r.put("status", 0);
		} catch (Exception e) {
			logger.error("System error occurs", e);
			r.put("status", -1);
			return r;
		}
		return r;
	}

}
