package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.Utils;
import com.blackcrystalinfo.platform.powersocket.bo.BizCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

import redis.clients.jedis.Jedis;

/**
 * 邀请家庭确认
 * 
 * @author Shenjz
 */
@Controller("/mobile/invitationcfm")
public class InvitationCfmApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(InvitationCfmApi.class);

	@Autowired
	IUserSvr loginSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String uId = req.getUserId();
		String oper = req.getParameter("uId");
		String asw = req.getParameter("asw");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			// 用户确认加入家庭是有时效限制的
//			String fId = j.get("user:invitationfamily:" + uId);
//			if (null == fId || !fId.equals(oper)) {
//				logger.info("confirm out date, uId:{}|oper:{}", uId, oper);
//				r.put("status", C0032.toString());
//				return r;
//			}
//			j.del("user:invitationfamily:" + uId);

			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();

			User operUser = loginSvr.getUser(User.UserIDColumn, oper);
			User familyUser = loginSvr.getUser(User.UserIDColumn, uId);

			String mnick = operUser.getNick();
			String nick = familyUser.getNick();

			mm.put("hostId", oper);
			mm.put("hostNick", mnick==null?operUser.getPhone():mnick);
			mm.put("mId", uId);
			mm.put("mNick", nick==null?familyUser.getPhone():nick);
			msg.append(JSON.toJSON(mm));

			if ("yes".equals(asw)) {
				String operFamily = j.hget("user:family", oper);
				if (StringUtils.isBlank(operFamily)) {
					j.hset("user:family", oper, oper);
					j.sadd("family:" + oper, oper);
				}
				j.hset("user:family", uId, oper);
				j.sadd("family:" + oper, uId);

				// 获取家庭所有设备
				Set<String> members = j.smembers("family:" + oper);
				String memlist = StringUtils.join(members.iterator(), ",") + "|";

				j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(memlist, BizCode.FamilyAddSuccess.getValue(), Long.parseLong(uId), msg.toString()));

				// 发布通知：用户设备列表更新
				pubDeviceUsersRels(uId, members, j);
				
				logger.info("{}|{}|{}",System.currentTimeMillis(),oper,new String(nick+"加入本家庭"));
			} else {
				j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(String.valueOf(oper) + "|", BizCode.FamilyRefuse.getValue(), Long.parseLong(uId), msg.toString()));
				j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(String.valueOf(uId) + "|", BizCode.FamilyRefuse.getValue(), Long.parseLong(uId), msg.toString()));

			}

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("", e);
			// DataHelper.returnBrokenJedis(j);
		} finally {
			DataHelper.returnJedis(j);
		}

		return r;
	}

	/**
	 * 加入家庭后，更新说有家庭成员的设备列表，及加入用户的设备列表。
	 * 
	 * @param uId
	 *            加入用户的ID
	 * @param members
	 *            加入家庭成员ID列表
	 * @param j
	 */
	private void pubDeviceUsersRels(String uId, Set<String> members, Jedis j) {
		Set<String> devices = j.smembers("u:" + uId + ":devices");
		for (String d : devices) {
			// 家庭所有成员需要更新列表
			for (String m : members) {
				// 自己的设备无需处理
				if (StringUtils.equals(m, uId)) {
					continue;
				}

				StringBuilder sb = new StringBuilder();
				sb.append(d).append("|").append(m).append("|").append("1");
				j.publish("PubDeviceUsers", sb.toString());
			}
		}

		for (String m : members) {
			// 自己的设备无需处理
			if (StringUtils.equals(m, uId)) {
				continue;
			}

			devices = j.smembers("u:" + m + ":devices");
			// 家庭下没个成员绑定的设备，都要更新到新用户的名下
			for (String d : devices) {
				StringBuilder sb = new StringBuilder();
				sb.append(d).append("|").append(uId).append("|").append("1");
				j.publish("PubDeviceUsers", sb.toString());
			}
		}
	}

}
