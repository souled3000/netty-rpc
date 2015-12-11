package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0003;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0005;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.Utils;
import com.blackcrystalinfo.platform.powersocket.bo.BizCode;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IDeviceSrv;

import redis.clients.jedis.Jedis;

@Controller("/mobile/unbind")
public class UserUnbindDeviceApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserUnbindDeviceApi.class);

	@Autowired
	private IDeviceSrv deviceDao;


	public Object rpc(RpcRequest req) throws Exception {
		String mac = req.getParameter("mac");
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String uId = req.getUserId();

		// 手机端需要mac区分绑定解绑的是哪个设备，这里给返回。是不是很奇葩。。。
		r.put("mac", mac);

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			if (!deviceDao.exists(mac)) {
				r.put(status, C0003.toString());
				return r;
			}

			String deviceId = String.valueOf(deviceDao.getIdByMac(mac));
			String owner = j.hget("device:owner", deviceId);
			if (owner == null || !StringUtils.equals(owner, uId)) {
				r.put(status, C0005.toString());
				return r;
			}

			j.hdel("device:owner", deviceId);
			j.srem("u:" + uId + ":devices", deviceId);

			StringBuilder sb = new StringBuilder();
			sb.append(deviceId).append("|").append(uId).append("|").append("0");
			j.publish("PubDeviceUsers", sb.toString());
			
			// 设备绑定解绑，发布通知消息，更新用户设备关系。
			String f = j.hget("user:family", uId);
			Set<String> all = new HashSet<String>();
			Set<String> members = j.smembers("family:" + f);
			all.addAll(members);
			all.add(uId);
			pubDeviceUsersRels(deviceId, all, j);

			// 更新设备控制密钥
			pushMsg2Dev(Long.valueOf(uId),Long.valueOf(deviceId), j);
		} catch (Exception e) {
			logger.error("", e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}

	/**
	 * 设备解绑，发布通知消息，更新用户设备关系。
	 * 
	 * @param dev
	 *            上下线设备ID
	 * @param m
	 *            用户列表
	 * @param jedis
	 *            redis连接
	 */
	private void pubDeviceUsersRels(String dev, Set<String> m, Jedis j) {

		// 用户没加入家庭
		if (null == m) {
			return;
		}

		// 家庭所有成员需要更新列表
		for (String o : m) {
			StringBuilder sb = new StringBuilder();
			sb.append(dev).append("|").append(o).append("|").append("0");
			j.publish("PubDeviceUsers", sb.toString());
			j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(o + "|", BizCode.DeviceUnBind.getValue(), Long.valueOf(dev),""));
		}
	}

	private void pushMsg2Dev(Long userId,Long devId, Jedis j) {
		byte[] ctlKey = CookieUtil.genCtlKey();
		j.hset("device:ctlkey:tmp".getBytes(), String.valueOf(devId).getBytes(), ctlKey);
		byte[] ctn = new byte[25];
		EndianUtils.writeSwappedLong(ctn, 0, devId);
		System.arraycopy(new byte[]{0x03}, 0, ctn, 8, 1);
		System.arraycopy(ctlKey, 0, ctn, 9, 16);
		j.publish(Constants.DEVCOMMONMSGCODE.getBytes(), ctn);
		ctn = new byte[17];
		EndianUtils.writeSwappedLong(ctn, 0, devId);
		System.arraycopy(new byte[]{0x02}, 0, ctn, 8, 1);
		EndianUtils.writeSwappedLong(ctn, 9, userId);
		j.publish(Constants.DEVCOMMONMSGCODE.getBytes(), ctn);
	}

}
