package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.powersocket.bo.BizCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.Utils;

/**
 * 
 * @author ShenJZ 解绑用户
 */
@Controller("/mobile/demission")
public class DemissionApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DemissionApi.class);
	@Autowired
	ILoginSvr loginSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String userId = req.getParameter("uId");
		String family = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			User user = null;
			User familyUser = null;

			try {
				user = loginSvr.userGet(User.UserIDColumn, userId);
				familyUser = loginSvr.userGet(User.UserIDColumn, family);
			} catch (Exception ex) {
				user = null;
			}

			if (null == user) {
				r.put(status, C0006.toString());
				return r;
			}

			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();
			String hostNick = familyUser.getNick();
			String nick = user.getNick();

			mm.put("hostId", family);
			mm.put("hostNick", hostNick);
			mm.put("mId", userId);
			mm.put("mNick", nick);
			msg.append(JSON.toJSON(mm));

			j.hdel("user:family", userId);
			j.srem("family:" + family, userId);

			Set<String> members = j.smembers("family:" + family);
			pubDeviceUsersRels(userId, members, j);

			// 当所有成员用户退出后自动解散家庭
			if (members.size() == 1) {
				j.hdel("user:family", family);
				j.del("family:" + family);
			}

			// 解绑用户，除了给家庭其他成员推送外，还需要给被解绑用户推送消息。
			members.add(userId);
			String memlist = StringUtils.join(members.iterator(), ",") + "|";

			j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(memlist, BizCode.FamilyRemoveMember.getValue(), Integer.parseInt(userId), msg.toString()));

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			r.put(status, SYSERROR.toString());
			logger.error("Bind in error uId:{}|family:{}|status:{}", userId, family, family, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		return r;
	}

	/**
	 * 退出家庭后，更新所有家庭成员的设备列表，及退出用户的设备列表。
	 * 
	 * @param uId
	 *            退出用户的ID
	 * @param members
	 *            退出家庭成员ID列表（不包含退出用户ID）
	 * @param j
	 */
	private void pubDeviceUsersRels(String uId, Set<String> members, Jedis j) {
		Set<String> devices = j.smembers("u:" + uId + ":devices");
		for (String d : devices) {
			// 家庭其他成员移除退出用户的设备
			for (String m : members) {
				StringBuilder sb = new StringBuilder();
				sb.append(d).append("|").append(m).append("|").append("0");
				j.publish("PubDeviceUsers", sb.toString());
			}

			// 更新设备控制密钥
			updateDeviceCtlKey(d, j);
		}

		for (String m : members) {
			devices = j.smembers("u:" + m + ":devices");
			// 退出用户的设备列表里移除家庭其他成员的设备
			for (String d : devices) {
				StringBuilder sb = new StringBuilder();
				sb.append(d).append("|").append(uId).append("|").append("0");
				j.publish("PubDeviceUsers", sb.toString());

				// 更新设备控制密钥
				updateDeviceCtlKey(d, j);
			}
		}
	}

	private void updateDeviceCtlKey(String devId, Jedis j) {
		// TODO 发布消息，通知设备更新控制密钥了
		String ctlKey = CookieUtil.generateDeviceCtlKey(devId);
		j.hset("device:ctlkey:tmp", devId, ctlKey);
		j.publish("PubDevCtlKeyUpdate", devId + "|" + ctlKey);
	}
}
