package com.github.com.nettyrpc.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

public class DevicePwdModifying implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(DevicePwdModifying.class);
	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		DevicePwdModifyingResponse resp = new DevicePwdModifyingResponse();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());
		
		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String newDevicePwd = HttpUtil.getPostValue(req.getParams(), "devicePwd");
		
		if(StringUtils.isBlank(mac)||StringUtils.isBlank(cookie)||StringUtils.isBlank(newDevicePwd)){
			resp.setStatus(1);
			return resp;
		}
		Jedis jedis = null;
		try{
			jedis = DataHelper.getJedis();
			String deviceId = jedis.hget("device:mactoid", mac);
			if(deviceId==null){
				resp.setStatus(1);
				return resp;
			}
			String[] cookies = CookieUtil.decode(cookie);
			String userId = cookies[0];
				
			jedis.hset("user:device:pwd", userId+"_"+deviceId, newDevicePwd);
			resp.setStatus(0);
		}catch(Exception e){
			resp.setStatus(-1);
			DataHelper.returnBrokenJedis(jedis);
			logger.info("",e);
			return resp;
		}finally{
			DataHelper.returnJedis(jedis);
		}
		return resp;
	}
	private class DevicePwdModifyingResponse extends ApiResponse{
		
	}
}
