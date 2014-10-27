package com.blackcrystalinfo.platform.powersocket.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import sun.misc.BASE64Decoder;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;

public class DeviceNameChangingHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DeviceNameChangingHandler.class);
	private BASE64Decoder decoder = new BASE64Decoder();

	public Object rpc(JSONObject req) throws InternalException {
		Response resp = new Response();
		String mac = req.getString("mac");
		String name = req.getString("name");
		try {
			name = new String(decoder.decodeBuffer(name.replace(' ', '+')), "utf8");
		} catch (UnsupportedEncodingException e) {
			resp.setStatus(-2);
			logger.error("UnsupportedEncodingException. mac:{}|DeviceName:{}|status:{}", mac, name, resp.getStatus(), e);
			return resp;
		} catch (IOException e) {
			resp.setStatus(-3);
			logger.error("IOException. mac:{}|DeviceName:{}|status:{}", mac, name, resp.getStatus(), e);
			return resp;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			String id = jedis.hget("device:mactoid", mac);
			if(id==null){
				resp.setStatus(1);
				logger.error("There isn't the Device. mac:{}|DeviceName:{}|status:{}", mac, name, resp.getStatus());
				return resp;
			}
			jedis.hset("device:name", id, name);
		} catch (Exception e) {
			resp.setStatus(-1);
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Geting user's device occurs an error. mac:{}|DeviceName:{}|status:{}", mac, name, resp.getStatus(), e);
			return resp;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		resp.setStatus(0);
		logger.info("response: mac:{}|DeviceName:{}|status:{}", mac, name, resp.getStatus());
		return resp;
	}

	private class Response extends ApiResponse {

	}
}
