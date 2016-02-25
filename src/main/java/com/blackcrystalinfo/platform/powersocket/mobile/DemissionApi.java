package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
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

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.common.BizCode;
import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
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
 * 
 * @author ShenJZ 解绑用户
 */
@Controller("/mobile/demission")
public class DemissionApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DemissionApi.class);
	@Autowired
	IUserSvr loginSvr;
	@Autowired
	ILogger log;
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String userId = req.getParameter("uId");
		String family = req.getUserId();
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			User member = null;
			User host = null;

			try {
				member = loginSvr.getUser(User.UserIDColumn, userId);
				host = loginSvr.getUser(User.UserIDColumn, family);
			} catch (Exception ex) {
				member = null;
			}

			if (null == member) {
				r.put(status, C0006.toString());
				return r;
			}

			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();

			host.setNick(host.getNick() == null ? host.getPhone() : host.getNick());
			member.setNick(member.getNick() == null ? member.getPhone() : member.getNick());

			mm.put("hostId", family);
			mm.put("hostNick", host.getNick());
			mm.put("mId", userId);
			mm.put("mNick", member.getNick());
			
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

			j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(memlist, BizCode.FamilyRemoveMember.getValue(), Integer.parseInt(userId), msg.toString()));
			writeLog(host, member, members);
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

	private void writeLog(User host, User member, Set<String> members) {
		StringBuilder logBuilder = new StringBuilder();
		logBuilder.append(member.getNick()).append("已被").append(host.getNick()).append("家庭移除");
		Long ts = System.currentTimeMillis();
		for(String m : members){
			if(!m.equals(host.getId())&&!m.equals(member.getId())){
				log.write(String.format("%s|%s|%s|%s", m,ts,LogType.JT,logBuilder.toString()));
			}
		}
		logBuilder.delete(0, logBuilder.length());
		logBuilder.append(member.getNick()).append("被从家庭中移除");
		log.write(String.format("%s|%s|%s|%s", host.getId(),ts,LogType.JT,logBuilder.toString()));
		logBuilder.delete(0, logBuilder.length());
		logBuilder.append("我").append("已被").append(host.getNick()).append("的家庭家庭移除");
		log.write(String.format("%s|%s|%s|%s", member.getId(),ts,LogType.JT,logBuilder.toString()));
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
		byte[] ctlKey = CookieUtil.gen16();
		j.hset("device:ctlkey:tmp".getBytes(), String.valueOf(devId).getBytes(), ctlKey);
		byte[] ctn = new byte[25];
		EndianUtils.writeSwappedLong(ctn, 0, devId);
		System.arraycopy(new byte[]{0x03}, 0, ctn, 8, 1);
		System.arraycopy(ctlKey, 0, ctn, 9, 16);
		j.publish(Constants.DEVCOMMONMSGCODE.getBytes(), ctn);
	}
}
