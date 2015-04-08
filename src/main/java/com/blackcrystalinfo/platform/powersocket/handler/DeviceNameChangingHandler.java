package com.blackcrystalinfo.platform.powersocket.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import sun.misc.BASE64Decoder;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.HttpUtil;
@Path(path="/api/device/changingname")
public class DeviceNameChangingHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DeviceNameChangingHandler.class);
	private BASE64Decoder decoder = new BASE64Decoder();

	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		String name = req.getString("name");
		return deal(mac, name);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String name = HttpUtil.getPostValue(req.getParams(), "name");
		return deal(mac, name);
	}
	
	public Object deal(String...args) throws InternalException {
		Map<Object,Object> r = new HashMap<Object,Object>();
		String mac = args[0];
		String name = args[1];
		try {
			name = new String(decoder.decodeBuffer(name.replace(' ', '+')), "utf8");
		} catch (UnsupportedEncodingException e) {
			r.put("status",-2);
			logger.error("UnsupportedEncodingException. mac:{}|DeviceName:{}|status:{}", mac, name, r.get("status"), e);
			return r;
		} catch (IOException e) {
			r.put("status",-3);
			logger.error("IOException. mac:{}|DeviceName:{}|status:{}", mac, name, r.get("status"), e);
			return r;
		}
		logger.info("DeviceNameChangingHandler begin mac:{}|name:{}",mac,name);

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			String id = jedis.hget("device:mactoid", mac);
			if(id==null){
				r.put("status",1);
				logger.error("There isn't the Device. mac:{}|DeviceName:{}|status:{}", mac, name, r.get("status"));
				return r;
			}
			jedis.hset("device:name", id, name);
		} catch (Exception e) {
			r.put("status",-1);
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Geting user's device occurs an error. mac:{}|DeviceName:{}|status:{}", mac, name, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		logger.info("response: mac:{}|DeviceName:{}|status:{}", mac, name, r.get("status"));
		r.put("status",0);
		return r;
	}

}