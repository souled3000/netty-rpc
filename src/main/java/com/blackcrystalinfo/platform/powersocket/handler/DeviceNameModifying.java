package com.blackcrystalinfo.platform.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class DeviceNameModifying implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(DeviceNameModifying.class);
	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		DeviceNameModifyingResponse resp = new DeviceNameModifyingResponse();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());
		
		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String newDeviceName = HttpUtil.getPostValue(req.getParams(), "deviceName");
		
		if(StringUtils.isBlank(mac)||StringUtils.isBlank(cookie)||StringUtils.isBlank(newDeviceName)){
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

			String key = "bind:user:"+userId;
			
			jedis.hset("device:name:"+userId,deviceId,newDeviceName);
			
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
	private class DeviceNameModifyingResponse extends ApiResponse{
		
	}
}
