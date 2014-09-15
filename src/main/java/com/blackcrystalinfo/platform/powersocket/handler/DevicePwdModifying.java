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

public class DevicePwdModifying implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(DevicePwdModifying.class);
	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		DevicePwdModifyingResponse resp = new DevicePwdModifyingResponse();
		resp.setStatus(-1);
		resp.setUrlOrigin(req.getUrlOrigin());
		
		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String newDevicePwd = HttpUtil.getPostValue(req.getParams(), "devicePwd");
		
		logger.info("DevicePwdModifying bgein mac:{}|cookie:{}|newDevicePwd:{}",mac,cookie,newDevicePwd);
		if(StringUtils.isBlank(mac)||StringUtils.isBlank(cookie)||StringUtils.isBlank(newDevicePwd)){
			resp.setStatus(1);
			logger.info("something is null mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,resp.getStatus());
			return resp;
		}
		Jedis jedis = null;
		try{
			jedis = DataHelper.getJedis();
			String deviceId = jedis.hget("device:mactoid", mac);
			if(deviceId==null){
				resp.setStatus(2);
				logger.info("no matching device mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,resp.getStatus());
				return resp;
			}
			String[] cookies = CookieUtil.decode(cookie);
			String userId = cookies[0];
				
			jedis.hset("device:pwd:"+userId,deviceId,newDevicePwd);
			resp.setStatus(0);
		}catch(Exception e){
			resp.setStatus(-1);
			DataHelper.returnBrokenJedis(jedis);
			logger.info("mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,resp.getStatus(),e);
			return resp;
		}finally{
			DataHelper.returnJedis(jedis);
		}
		logger.info("response: mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,resp.getStatus());
		return resp;
	}
	private class DevicePwdModifyingResponse extends ApiResponse{
		
	}
}
