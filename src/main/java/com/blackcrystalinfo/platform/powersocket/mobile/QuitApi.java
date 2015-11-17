package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0029;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0030;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.Utils;
import com.blackcrystalinfo.platform.powersocket.bo.BizCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

/**
 * 
 * @author ShenJZ 用户退出家庭
 */
@Controller("/mobile/quit")
public class QuitApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(QuitApi.class);

	@Autowired
	IUserSvr loginSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String uId = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String fId = j.hget("user:family", uId);

			if (StringUtils.isBlank(fId)) {
				r.put(status, C0029.toString());
				logger.debug("要退出的人:{},家庭:{}", uId, fId);
				return r;
			}

			if (StringUtils.equals(fId, uId)) {
				logger.debug("要退出的人:{},家庭:{}", uId, fId);
				r.put(status, C0030.toString());
				return r;
			}

			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();

			User user = loginSvr.getUser(User.UserIDColumn, uId);
			User familyUser = loginSvr.getUser(User.UserIDColumn, fId);
			String hostNick = familyUser.getNick();
			String nick = user.getNick();

			mm.put("hostId", fId);
			mm.put("hostNick", hostNick);
			mm.put("mId", uId);
			mm.put("mNick", nick);
			msg.append(JSON.toJSON(mm));

			j.hdel("user:family", uId);
			j.srem("family:" + fId, uId);

			Set<String> members = j.smembers("family:" + fId);
			pubDeviceUsersRels(uId, members, j);

			// 当所有成员用户退出后自动解散家庭
			if (members.size() == 1) {
				j.hdel("user:family", fId);
				j.del("family:" + fId);
			}

			// 用户退出家庭，除了给家庭其他成员推送外，还需要给用户自己推送消息。
			members.add(uId);

			String memlist = StringUtils.join(members.iterator(), ",") + "|";

			j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(memlist, BizCode.FamilyQuit.getValue(), Integer.parseInt(uId), msg.toString()));
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
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
			pushMsg2Dev(Long.valueOf(d), j);
		}

		for (String m : members) {
			devices = j.smembers("u:" + m + ":devices");
			// 退出用户的设备列表里移除家庭其他成员的设备
			for (String d : devices) {
				StringBuilder sb = new StringBuilder();
				sb.append(d).append("|").append(uId).append("|").append("0");
				j.publish("PubDeviceUsers", sb.toString());

				// 更新设备控制密钥
				pushMsg2Dev(Long.valueOf(d), j);
			}
		}
	}

	private void pushMsg2Dev(Long devId, Jedis j) {
		byte[] ctlKey = CookieUtil.generateDeviceCtlKey("");
		j.hset("device:ctlkey:tmp".getBytes(), String.valueOf(devId).getBytes(), ctlKey);
		byte[] ctn = new byte[25];
		EndianUtils.writeSwappedLong(ctn, 0, devId);
		System.arraycopy(new byte[]{0x03}, 0, ctn, 8, 1);
		System.arraycopy(ctlKey, 0, ctn, 9, 16);
		j.publish(Constants.DEVCOMMONMSGCODE.getBytes(), ctn);
	}
}
