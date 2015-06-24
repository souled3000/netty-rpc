package com.blackcrystalinfo.platform.powersocket.api;

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

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
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
@Path(path = "/mobile/invitationcfm")
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
				for (String m : members) {
					Set<String> devices = j.smembers("u:" + m + ":devices");
					for (String o : devices) {
						StringBuilder sb = new StringBuilder();
						// 将uid与oper下的所有设备做关联
						sb.append(o).append("|").append(uId).append("|").append("1");
						j.publish("PubDeviceUsers", sb.toString());
					}
				}
				String memlist = StringUtils.join(members.iterator(), ",") + "|";

				j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(memlist, BizCode.FamilyAddSuccess.getValue() , Long.parseLong(uId), msg.toString()));
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
}
