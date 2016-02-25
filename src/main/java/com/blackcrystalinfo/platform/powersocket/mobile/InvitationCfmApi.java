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
import com.blackcrystalinfo.platform.common.BizCode;
import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.LogType;
import com.blackcrystalinfo.platform.common.Utils;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.powersocket.log.ILogger;
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
	@Autowired
	ILogger log;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String uId = req.getUserId();
		String hostId = req.getParameter("uId");
		String asw = req.getParameter("asw");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			// 用户确认加入家庭是有时效限制的
			// String fId = j.get("user:invitationfamily:" + uId);
			// if (null == fId || !fId.equals(oper)) {
			// logger.info("confirm out date, uId:{}|oper:{}", uId, oper);
			// r.put("status", C0032.toString());
			// return r;
			// }
			// j.del("user:invitationfamily:" + uId);

			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();

			User host = loginSvr.getUser(User.UserIDColumn, hostId);
			User member = loginSvr.getUser(User.UserIDColumn, uId);

			host.setNick(host.getNick() == null ? host.getPhone() : host.getNick());
			member.setNick(member.getNick() == null ? member.getPhone() : member.getNick());
			
			mm.put("hostId", hostId);
			mm.put("hostNick", host.getNick());
			mm.put("mId", uId);
			mm.put("mNick", member.getNick());
			msg.append(JSON.toJSON(mm));

			if ("yes".equals(asw)) {
				String operFamily = j.hget("user:family", hostId);
				if (StringUtils.isBlank(operFamily)) {
					j.hset("user:family", hostId, hostId);
					j.sadd("family:" + hostId, hostId);
				}
				j.hset("user:family", uId, hostId);
				j.sadd("family:" + hostId, uId);

				// 获取家庭所有设备
				Set<String> members = j.smembers("family:" + hostId);
				String memlist = StringUtils.join(members.iterator(), ",") + "|";

				j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(memlist, BizCode.FamilyAddSuccess.getValue(), Long.parseLong(uId), msg.toString()));

				// 发布通知：用户设备列表更新
				pubDeviceUsersRels(uId, members, j);
				writeLog(host, member, members);
			} else {
				j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(String.valueOf(hostId) + "|", BizCode.FamilyRefuse.getValue(), Long.parseLong(uId), msg.toString()));
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



	private void writeLog(User host, User member, Set<String> members) {
		StringBuilder logBuilder = new StringBuilder();
		logBuilder.append(member.getNick()).append("成功加入到").append(host.getNick()).append("家庭");
		Long ts = System.currentTimeMillis();
		for(String m : members){
			if(!m.equals(host.getId())&&!m.equals(member.getId())){
				log.write(String.format("%s|%s|%s|%s", m,ts,LogType.JT,logBuilder.toString()));
			}
		}
		logBuilder.delete(0, logBuilder.length());
		logBuilder.append(member.getNick()).append("加入本家庭");
		log.write(String.format("%s|%s|%s|%s", host.getId(),ts,LogType.JT,logBuilder.toString()));
		logBuilder.delete(0, logBuilder.length());
		logBuilder.append("我").append("加入").append(host.getNick()).append("的家庭");
		log.write(String.format("%s|%s|%s|%s", member.getId(),ts,LogType.JT,logBuilder.toString()));
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
			// 家庭下每个成员绑定的设备，都要更新到新用户的名下
			for (String d : devices) {
				StringBuilder sb = new StringBuilder();
				sb.append(d).append("|").append(uId).append("|").append("1");
				j.publish("PubDeviceUsers", sb.toString());
			}
		}
	}
}
