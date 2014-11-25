package com.blackcrystalinfo.platform.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;
import com.blackcrystalinfo.platform.util.CometScanner;

public class GeliIPRefreshHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(WebsocketInfoHandler.class);

	public Object rpc(RpcRequest req) throws InternalException {
		GeLiIPResp resp = new GeLiIPResp();
		try {
			CometScanner.refresh();
			resp.setStatus(0);
		} catch (Exception e) {
			logger.error("System error occurs", e);
			resp.setStatus(-1);
			return resp;
		}
		return resp;
	}

	private class GeLiIPResp extends ApiResponse {
	}
}
