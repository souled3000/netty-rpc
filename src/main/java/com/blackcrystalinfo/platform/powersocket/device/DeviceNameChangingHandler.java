package com.blackcrystalinfo.platform.powersocket.device;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;
import com.blackcrystalinfo.platform.service.InternalException;

@Controller("/api/device/changingname")
public class DeviceNameChangingHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DeviceNameChangingHandler.class);

	@Autowired
	private IDeviceSrv deviceDao;

	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		String name = req.getString("name");
		return deal(mac, name);
	}

	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter("mac");
		String name = req.getParameter("name");
		return deal(mac, name);
	}

	public Object deal(String... args) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String mac = args[0];
		String name = args[1];
		logger.info("chang name begin, mac:{}|name:{}", mac, name);

		try {
			Long id = deviceDao.getIdByMac(mac);
			if (id == null) {
				r.put("status", 1);
				logger.error("Device not exist, mac:{}", mac);
				return r;
			}
			deviceDao.setNameById(id, name);
		} catch (Exception e) {
			r.put("status", -1);
			logger.error("change name error, mac:{}|name:{}|e:{}", mac, name, e);
			return r;
		}
		r.put("status", 0);
		return r;
	}

}