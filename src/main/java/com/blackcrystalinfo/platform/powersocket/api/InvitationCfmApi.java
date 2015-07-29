package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0032;
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
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.BizCode;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.Utils;

/**
 * 邀请家庭确认
 * 
 * @author Shenjz
 */
@Controller("/mobile/invitationcfm")
public class InvitationCfmApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(InvitationCfmApi.class);
	
	@Autowired
	ILoginSvr loginSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String uId = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));
		String oper = req.getParameter("uId");
		String asw = req.getParameter("asw");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			// 用户确认加入家庭是有时效限制的
			String fId = j.get("user:invitationfamily:" + uId);
			if (null == fId || !fId.equals(oper)) {
				logger.info("confirm out date, uId:{}|oper:{}", uId, oper);
				r.put("status", C0032.toString());
				return r;
			}
			j.del("user:invitationfamily:" + uId);

			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();
			
			User operUser = loginSvr.userGet(User.UserIDColumn, oper);
			User familyUser = loginSvr.userGet(User.UserIDColumn, uId);
			
			String mnick = operUser.getNick();
			String nick = familyUser.getNick();
			
			mm.put("hostId", oper);
			mm.put("hostNick", mnick);
			mm.put("mId", uId);
			mm.put("mNick", nick);
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

				j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(memlist, BizCode.FamilyAddSuccess.getValue() , Long.parseLong(uId), msg.toString()));

				// 发布通知：用户设备列表更新
				pubDeviceUsersRels(oper, members, j);
			} else {
				j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(String.valueOf(oper) + "|", BizCode.FamilyRefuse.getValue() , Long.parseLong(uId), msg.toString()));

			}
			
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("", e);
			//DataHelper.returnBrokenJedis(j);
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
				StringBuilder sb = new StringBuilder();
				sb.append(d).append("|").append(m).append("|").append("1");
				j.publish("PubDeviceUsers", sb.toString());
			}
		}

		for (String m : members) {
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
