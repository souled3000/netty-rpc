package com.blackcrystalinfo.platform.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;
import com.blackcrystalinfo.platform.util.CometScanner;

public class GeLiWebsocketHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(WebsocketInfoHandler.class);

	public Object rpc(RpcRequest req) throws InternalException {
		GeLiUrlResp resp = new GeLiUrlResp();
		// 1. 校验cookie信息
		try {
//			resp.setProxyAddr(CometScannerV2.take());
			resp.setProxyAddr(CometScanner.take());
			resp.setStatus(0);
		} catch (Exception e) {
			logger.error("System error occurs", e);
			resp.setStatus(-1);
			return resp;
		}
		logger.info("response: status:{}|proxyAddr:{}", resp.getStatus(), resp.getProxyAddr());
		return resp;
	}

	private class GeLiUrlResp extends ApiResponse {
		private String proxyAddr;

		public String getProxyAddr() {
			return proxyAddr;
		}

		public void setProxyAddr(String proxyAddr) {
			this.proxyAddr = proxyAddr;
		}
	}
}
