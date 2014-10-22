package com.blackcrystalinfo.platform.powersocket.handler;

import java.security.MessageDigest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class DeviceNameModifying extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DeviceNameModifying.class);
	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		DeviceNameModifyingResponse resp = new DeviceNameModifyingResponse();
		resp.setStatus(-1);
		resp.setUrlOrigin(req.getUrlOrigin());
		
		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");//手机端cookie
		String newDeviceName = HttpUtil.getPostValue(req.getParams(), "deviceName");
		
		logger.info("DeviceNameModifying begin mac:{}|cookie:{}|newDeviceName:{}",mac,cookie,newDeviceName);
		
		if(StringUtils.isBlank(mac)||StringUtils.isBlank(cookie)||StringUtils.isBlank(newDeviceName)){
			resp.setStatus(2);
			logger.info("something is null. mac:{}|cookie:{}|newDeviceName:{}|status:{}",mac,cookie,newDeviceName,resp.getStatus());
			return resp;
		}
		
		String[] cs = cookie.split("-");
		
		if(cs.length!=2){
			resp.setStatus(3);
			logger.info("cookies[] length !=2",mac,cookie,newDeviceName,resp.getStatus());
			return resp;
		}
		
		Jedis jedis = null;
		try{
			jedis = DataHelper.getJedis();
			String deviceId = jedis.hget("device:mactoid", mac);
			if(deviceId==null){
				resp.setStatus(1);
				logger.info("no matching device. mac:{}|cookie:{}|newDeviceName:{}|status:{}",mac,cookie,newDeviceName,resp.getStatus());
				return resp;
			}
			String[] cookies = CookieUtil.decode(cs[0]);
			String userId = cookies[0];

			String shadow = jedis.hget("user:shadow", userId);
			String csmd5=cs[1];
			String csmd52 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId+shadow).getBytes()));
			
			if(!csmd5.equals(csmd52)){
				resp.setStatus(7);
				logger.info("user:shadow don't match user's ID. mac:{}|cookie:{}|newDeviceName:{}|status:{} ",mac,cookie,newDeviceName,resp.getStatus());
				return resp;
			}
			
//			String key = "bind:user:"+userId;
			
			jedis.hset("device:name:"+userId,deviceId,newDeviceName);
			
			resp.setStatus(0);
		}catch(Exception e){
			
			DataHelper.returnBrokenJedis(jedis);
			logger.info(" mac:{}|cookie:{}|newDeviceName:{}|status:{}",mac,cookie,newDeviceName,resp.getStatus(),e);
			return resp;
		}finally{
			DataHelper.returnJedis(jedis);
		}
		logger.info("response:  mac:{}|cookie:{}|newDeviceName:{}|status:{}",mac,cookie,newDeviceName,resp.getStatus());
		return resp;
	}
	private class DeviceNameModifyingResponse extends ApiResponse{
		
	}
}
