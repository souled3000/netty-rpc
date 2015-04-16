package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import sun.misc.BASE64Decoder;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.api.UserLoginApi;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

@Path(path="/api/device/register")
public class DeviceRegisterHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserLoginApi.class);

	@Override
	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		String sn = req.getString("sn");
		String dv = req.getString("dv");
		String pid = req.getString("pid");
		String name = req.getString("name");
		return deal(mac,sn,dv,pid,name);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter( "mac");
		String sn = req.getParameter( "sn");
		String dv = req.getParameter( "dv");
		String pid = req.getParameter( "pid");
		String name = req.getParameter( "name");
		return deal(mac,sn,dv,pid,name);
	}
	
	private Object deal(String... args)throws InternalException{
		BASE64Decoder decoder = new BASE64Decoder();
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put("status",-1);
		// result.setStatusMsg("");
		// result.setUrlOrigin(req.getUrlOrigin());

		String mac = args[0];
		String sn = args[1];
		String dv = args[2];
		String pid = args[3];
		String name = args[4];
		
		String regTime = String.valueOf(System.currentTimeMillis());
		logger.info("DeviceRegisterHandler begin mac:{}|sn:{}|bv:{}", mac, sn, dv);
		if (!isValidSN(sn)) {
			return r;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 设备MAC是否已被注册
			String existId = jedis.hget("device:mactoid", mac);
			if (null != existId) {
				// result.setStatusMsg("Regist failed! mac exists.");

				String cookie = CookieUtil.generateDeviceKey(mac, existId);
				// String timeStamp =
				// String.valueOf(System.currentTimeMillis());
				// String proxyKey = CookieUtil.generateKey(existId, timeStamp,
				// mac);
				// String proxyAddr = CookieUtil.getWebsocketAddr();

				r.put("status",1);
				// result.setDeviceId(existId);
				r.put("cookie",cookie);
			} else {
				// 2. 生成设备ID
				String deviceId = String.valueOf(jedis.decr("device:nextid"));

				Transaction tx = jedis.multi();

				// 3. 记录设备Id
				tx.hset("device:mactoid", mac, deviceId);

				// 4. 记录MAC地址
				tx.hset("device:mac", deviceId, mac);
				tx.hset("device:mac2", deviceId, ByteUtil.toHex(decoder.decodeBuffer(mac.replace(' ', '+'))));

				// 5. 记录设备SN号
				tx.hset("device:sn", deviceId, sn);

				// 6. 设备注册时间
				tx.hset("device:regtime", deviceId, regTime);

				// 7. 设备名称
				if(StringUtils.isNotBlank(name))
				tx.hset("device:name", deviceId, name);

				// 8. 设备类型
				tx.hset("device:dv", deviceId, dv);

				// 保存设备网关
				if (StringUtils.isNotBlank(pid))
					tx.hset("device:pid", deviceId, pid);

				String cookie = CookieUtil.generateDeviceKey(mac, deviceId);
				// String timeStamp =
				// String.valueOf(System.currentTimeMillis());
				// String proxyKey = CookieUtil.generateKey(deviceId, timeStamp,
				// mac);
				// String proxyAddr = CookieUtil.getWebsocketAddr();

				// result.setDeviceId(deviceId);
				r.put("cookie",cookie);
				tx.exec();
			}

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Device regist error mac:{}|sn:{}|bv:{}", mac, sn, dv, e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response:  mac:{}|sn:{}|bv:{}", mac, sn, dv);
		r.put("status", 0);
		return r;
	}
	
	private boolean isValidSN(String sn) {
		return true;
	}

	public static void main(String[] args) throws Exception {
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] bs = decoder.decodeBuffer("Dgw1DAD6");
		System.out.println(new String(bs, "utf8"));
		System.out.println(ByteUtil.toHex(bs));
	}
}
