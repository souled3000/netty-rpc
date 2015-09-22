package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.dao.IDeviceDao;
import com.blackcrystalinfo.platform.powersocket.data.Device;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

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
	private IDeviceDao deviceDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String deviceId = req.getParameter("deviceId");

		String userId = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String owner = j.hget("device:owner", deviceId);

			if (!userId.equals(owner)) {
				// TODO 设备Id不存在
				r.put(status, "");
				return r;
			}

			Map<Object, Object> devData = new HashMap<Object, Object>();
			Device device = deviceDao.get(deviceId);
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
		} catch (Exception e) {
			logger.error("", e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
