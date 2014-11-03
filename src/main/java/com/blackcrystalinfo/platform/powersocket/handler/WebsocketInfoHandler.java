package com.blackcrystalinfo.platform.powersocket.handler;

import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class WebsocketInfoHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketInfoHandler.class);

	public Object rpc(RpcRequest req) throws InternalException {
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		WebsocketInfoHandlerResponse resp = new WebsocketInfoHandlerResponse();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());
		
		logger.info("WebsocketInfoHandler begin cookie:{}",cookie);
		String[] cs = cookie.split("-");
		
		if(cs.length!=2){
			resp.setStatus(3);
			logger.info("UserDevices cookie:{}|status:{}",cookie,resp.getStatus());
			return resp;
		}
		// 1. 校验cookie信息
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			String[] cookies = CookieUtil.decode(cs[0]);
			String userId = cookies[0];

			String shadow = jedis.hget("user:shadow", userId);
			String csmd5=cs[1];
			String csmd52 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId+shadow).getBytes()));
			
			if(!csmd5.equals(csmd52)){
				resp.setStatus(7);
				return resp;
			}
			
			String heartBeat = CookieUtil.EXPIRE_SEC;
			String proxyKey = CookieUtil.generateKey(userId, String.valueOf(System.currentTimeMillis()/1000), CookieUtil.EXPIRE_SEC);
			
//			resp.setProxyAddr(CookieUtil.WEBSOCKET_ADDR);
			resp.setProxyAddr(CometScanner.take());
			logger.info("proxykey:{} | size:{} | proxyAddr:{} ", proxyKey, proxyKey.getBytes().length, resp.getProxyAddr());
			resp.setHeartBeat(heartBeat);
			resp.setProxyKey(proxyKey);
			resp.setStatus(0);
		} catch (Exception e) {
			logger.error("System error occurs",e);
			resp.setStatus(-1);
			return resp;
		}
		logger.info("response: status:{}|proxyKey:{}|proxyAddr:{}|heartBeat:{}", resp.getStatus(),resp.getProxyKey(),resp.getProxyAddr(),resp.getHeartBeat());
		return resp;
	}

	private class WebsocketInfoHandlerResponse extends ApiResponse {
		private String proxyAddr;
		private String proxyKey;
		private String heartBeat;
		public String getProxyAddr() {
			return proxyAddr;
		}
		public void setProxyAddr(String proxyAddr) {
			this.proxyAddr = proxyAddr;
		}
		public String getProxyKey() {
			return proxyKey;
		}
		public void setProxyKey(String proxyKey) {
			this.proxyKey = proxyKey;
		}
		public String getHeartBeat() {
			return heartBeat;
		}
		public void setHeartBeat(String heartBeat) {
			this.heartBeat = heartBeat;
		}
		
	}
}
