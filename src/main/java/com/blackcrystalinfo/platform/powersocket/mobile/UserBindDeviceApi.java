package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0004;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.EndianUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;

import redis.clients.jedis.Jedis;

/**
 * 添加设备到家庭
 * 
 * @author juliana
 * 
 */
@Controller("/mobile/bind")
public class UserBindDeviceApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserBindDeviceApi.class);

	@Autowired
	private IDeviceSrv deviceSrv;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String mac = req.getParameter("mac");
		String userId = req.getUserId();

		logger.info("begin BindHandler  user:{}|mac:{}", userId, mac);

		String deviceId = "";

		// 手机端需要mac区分绑定解绑的是哪个设备，这里给返回。是不是很奇葩。。。
		r.put("mac", mac);

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			if (!deviceSrv.exists(mac)) {
				r.put(status, C0003.toString());
				logger.info("There isn't this device. mac:{}|user:{}|status:{}", mac, userId, r.get(status));
				return r;
			}

			deviceId = String.valueOf(deviceSrv.getIdByMac(mac));
			String owner = j.hget("device:owner", deviceId);
			if (owner != null) {
				r.put(status, C0004.toString());
				logger.info("The device has been binded! mac:{}|user:{}|status:{}", mac, userId, r.get(status));
				return r;
			} else {
				j.hset("device:owner", deviceId, userId);
				j.hset("device:bindtime", deviceId, String.valueOf(System.currentTimeMillis()));
				j.sadd("u:" + userId + ":devices", deviceId);
			}

			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(userId).append("|").append("1");
			j.publish("PubDeviceUsers", sb.toString());

			// 设备绑定解绑，发布通知消息，更新用户设备关系。
			pubDeviceUsersRels(deviceId, j.smembers("family:" + userId), j);

			// 更新设备控制密钥
			pushMsg2Dev(Long.valueOf(userId),Long.valueOf(deviceId), j);
		} catch (Exception e) {
			logger.error("Bind in error mac:{}|user:{}|status:{}", mac, userId, r.get(status), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}

	/**
	 * 设备绑定解绑，发布通知消息，更新用户设备关系。
	 * 
	 * @param devId 上下线设备ID
	 * @param uIds 用户列表
	 * @param jedis redis连接
	 */
	private void pubDeviceUsersRels(String devId, Set<String> uIds, Jedis jedis) {

		// 用户没加入家庭
		if (null == uIds) {
			return;
		}

		// 家庭所有成员需要更新列表
		for (String uId : uIds) {
			StringBuilder sb = new StringBuilder();
			sb.append(devId).append("|").append(uId).append("|").append("1");
			jedis.publish("PubDeviceUsers", sb.toString());
		}
	}

	private void pushMsg2Dev(Long userId,Long devId, Jedis j) {
		byte[] ctlKey = CookieUtil.genCtlKey(String.valueOf(devId));
		j.hset("device:ctlkey:tmp".getBytes(), String.valueOf(devId).getBytes(), ctlKey);
		byte[] ctn = new byte[25];
		EndianUtils.writeSwappedLong(ctn, 0, devId);
		System.arraycopy(new byte[]{0x03}, 0, ctn, 8, 1);
		System.arraycopy(ctlKey, 0, ctn, 9, 16);
		j.publish(Constants.DEVCOMMONMSGCODE.getBytes(), ctn);
		ctn = new byte[17];
		EndianUtils.writeSwappedLong(ctn, 0, devId);
		System.arraycopy(new byte[]{0x01}, 0, ctn, 8, 1);
		EndianUtils.writeSwappedLong(ctn, 9, userId);
		j.publish(Constants.DEVCOMMONMSGCODE.getBytes(), ctn);
	}
}
