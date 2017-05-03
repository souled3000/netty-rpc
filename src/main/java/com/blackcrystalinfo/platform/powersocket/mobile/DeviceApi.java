package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.powersocket.bo.Device;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;

import redis.clients.jedis.Jedis;

/**
 * 获取单个设备的设备信息
 * 
 * @author j
 * 
 */
@Controller("/mobile/device")
public class DeviceApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DeviceApi.class);

	@Autowired
	private IDeviceSrv deviceSrv;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String deviceId = req.getParameter("dId");

		if (StringUtils.isBlank(deviceId)) {
			r.put(status, C0003.toString());
			return r;
		}

//		String userId = req.getUserId();

		Jedis j = null;
		try {
			j = JedisHelper.getJedis();

			Device device = deviceSrv.get(deviceId);
			String mac = device.getMac();
			String name = device.getName();
			String pwd = j.hget("device:ctlkey", deviceId);
			String dv = device.getDeviceType();
			String addr = j.hget("device:adr", deviceId);

			name = (null == name ? "" : name);

			r.put("id", deviceId);
			r.put("mac", mac);
			r.put("name", name);
			r.put("pwd", pwd);
			r.put("type", dv);
			r.put("addr", addr);
			r.put("owner",j.hget("device:owner", deviceId));
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("get device info error.", e);
		} finally {
			JedisHelper.returnJedis(j);
		}

		return r;
	}
}
