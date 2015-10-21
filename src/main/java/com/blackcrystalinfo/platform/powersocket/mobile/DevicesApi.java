package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.powersocket.bo.Device;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Controller("/mobile/devices")
public class DevicesApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DevicesApi.class);

	@Autowired
	private IDeviceSrv deviceDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String userId = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));

		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			List<Map<Object, Object>> bindedDevices = new ArrayList<Map<Object, Object>>();

			String family = j.hget("user:family", userId);
			if (StringUtils.isNotBlank(family)) {
				Set<String> mems = j.smembers("family:" + family);
				for (String m : mems) {
					Set<String> devices = j.smembers("u:" + m + ":devices");
					for (String id : devices) {
						Map<Object, Object> devData = new HashMap<Object, Object>();

						Device device = deviceDao.get(Long.valueOf(id));
						String mac = device.getMac();
						String name = device.getName();
						String pwd = j.hget("device:ctlkey", id);
						String dv = device.getDeviceType();
						String addr = j.hget("device:adr", id);

						name = (null == name ? "default" : name);

						devData.put("deviceId", id);
						devData.put("mac", mac);
						devData.put("deviceName", name);
						devData.put("devicePwd", pwd);
						devData.put("deviceType", dv);
						devData.put("deviceAddr", addr);
						bindedDevices.add(devData);
					}
				}
			} else {
				Set<String> devices = j.smembers("u:" + userId + ":devices");
				for (String id : devices) {
					Map<Object, Object> devData = new HashMap<Object, Object>();

					Device device = deviceDao.get(Long.valueOf(id));
					String mac = device.getMac();
					String name = device.getName();
					String pwd = j.hget("device:ctlkey", id);
					String dv = device.getDeviceType();
					String owner = j.hget("device:owner", id);
					String addr = j.hget("device:adr", id);

					name = (null == name ? "default" : name);

					devData.put("deviceId", id);
					devData.put("mac", mac);
					devData.put("deviceName", name);
					devData.put("devicePwd", pwd);
					devData.put("deviceType", dv);
					devData.put("owner", owner);
					devData.put("deviceAddr", addr);
					bindedDevices.add(devData);
				}
			}

			r.put("bindedDevices", bindedDevices);
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
