package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0005;
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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.powersocket.bo.Device;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;

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

		String deviceId = req.getParameter("deviceId");

		if (StringUtils.isBlank(deviceId)) {
			r.put(status, C0003.toString());
			return r;
		}

		String userId = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String owner = j.hget("device:owner", deviceId);
			if (!userId.equals(owner)) {
				r.put(status, C0005.toString());
				return r;
			}

			Map<Object, Object> devData = new HashMap<Object, Object>();
			Device device = deviceSrv.get(deviceId);
			String mac = device.getMac();
			String name = device.getName();
			String pwd = j.hget("device:ctlkey", deviceId);
			String dv = device.getDeviceType();
			String addr = j.hget("device:adr", deviceId);

			name = (null == name ? "default" : name);

			devData.put("deviceId", deviceId);
			devData.put("mac", mac);
			devData.put("deviceName", name);
			devData.put("devicePwd", pwd);
			devData.put("deviceType", dv);
			devData.put("deviceAddr", addr);
			r.put("deviceInfo", devData);
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("get device info error.", e);
		} finally {
			DataHelper.returnJedis(j);
		}

		return r;
	}
}
