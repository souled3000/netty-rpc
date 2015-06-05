package com.blackcrystalinfo.platform.powersocket.handler;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class DevicePwdModifying extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DevicePwdModifying.class);
	
	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		String cookie = req.getString("cookie");
		String devicePwd = req.getString("devicePwd");
		return deal(mac, cookie,devicePwd);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter( "mac");
		String cookie = req.getParameter( "cookie");
		String devicePwd = req.getParameter( "devicePwd");
		return deal(mac, cookie,devicePwd);
	}
	
	public Object deal(String...args) throws InternalException {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put("status",-1);
		
		String mac =args[0];
		String cookie =args[1];
		String newDevicePwd = args[2];
		
		logger.info("DevicePwdModifying bgein mac:{}|cookie:{}|newDevicePwd:{}",mac,cookie,newDevicePwd);
		if(StringUtils.isBlank(mac)||StringUtils.isBlank(cookie)||StringUtils.isBlank(newDevicePwd)){
			r.put("status",1);
			logger.info("something is null mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,r.get("status"));
			return r;
		}
		
		String[] cs = cookie.split("-");
		
		if(cs.length!=2){
			r.put("status",3);
			return r;
		}
		
		Jedis jedis = null;
		try{
			jedis = DataHelper.getJedis();
			String deviceId = jedis.hget("device:mactoid", mac);
			if(deviceId==null){
				r.put("status",2);
				logger.info("no matching device mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,r.get("status"));
				return r;
			}
			String userId = CookieUtil.gotUserIdFromCookie(cookie);

			String shadow = jedis.hget("user:shadow", userId);
			String csmd5=cs[1];
			String csmd52 = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId+shadow).getBytes()));
			
			if(!csmd5.equals(csmd52)){
				r.put("status",7);
				logger.info("user:shadow don't match user's ID. mac:{}|cookie:{}|status:{} ",mac,cookie,r.get("status"));
				return r;
			}
			
			jedis.hset("device:pwd:"+userId,deviceId,newDevicePwd);
			r.put("status",0);
		}catch(Exception e){
			r.put("status",-1);
			//DataHelper.returnBrokenJedis(jedis);
			logger.error("mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,r.get("status"),e);
			return r;
		}finally{
			DataHelper.returnJedis(jedis);
		}
		logger.info("response: mac:{}|cookie:{}|newDevicePwd:{}|status:{}",mac,cookie,newDevicePwd,r.get("status"));
		return r;
	}
}
