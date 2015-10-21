package com.blackcrystalinfo.platform.powersocket.device;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;
import com.blackcrystalinfo.platform.service.InternalException;

/**
 * 设备的控制密钥同步确认接口
 * 
 * 调用此接口后，设备的密钥才会正式生效。
 * 
 * @author j
 * 
 */
@Controller("/api/device/ctlkeysynchcfm")
public class DeviceCtlKeySynchCfmApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DeviceCtlKeySynchCfmApi.class);

	@Autowired
	private IDeviceSrv deviceDao;

	@Override
	public Object rpc(JSONObject req) throws InternalException {
		String mac = req.getString("mac");
		return deal(mac);
	}

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter("mac");
		return deal(mac);
	}

	public Object deal(String... args) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String mac = args[0];
		logger.info("synch ctl key cfm begin, mac:{}", mac);

		Jedis jedis = null;
		try {

			jedis = DataHelper.getJedis();
			Long id = deviceDao.getIdByMac(mac);
			if (id == null) {
				r.put("status", 1);
				logger.error("Device not exist, mac:{}", mac);
				return r;
			}

			String ctlKey = jedis.hget("device:ctlKey", id.toString());
			String tmp = jedis.hget("device:ctlkey:tmp", id.toString());

			if (StringUtils.equals(ctlKey, tmp)) {
				// 不需要同步
				r.put("status", 0);
				logger.error("Ctrl key has synchronized, mac:{}", mac);
				return r;
			}

			// 执行同步操作
			jedis.hset("device:ctlkey", id.toString(), (null == tmp ? "" : tmp));
		} catch (Exception e) {
			r.put("status", -1);
			logger.error("synch ctl key cfm error, mac:{}|e:{}", mac, e);
			return r;
		}
		r.put("status", 0);
		return r;
	}

}
