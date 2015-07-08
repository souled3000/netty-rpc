package com.blackcrystalinfo.platform.powersocket.api;

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
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.BizCode;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.Utils;

/**
 * 
 * @author ShenJZ
 *	用户退出家庭
 */
@Controller("/mobile/quit")
public class QuitApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(QuitApi.class);

	@Autowired
	ILoginSvr loginSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String uId = CookieUtil.gotUserIdFromCookie(req.getParameter( "cookie"));
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String fId = j.hget("user:family", uId);
			
			if(StringUtils.isBlank(fId)){
				r.put(status, ErrorCode.C0029.toString());
				logger.debug("要退出的人:{},家庭:{}",uId,fId);
				return r;
			}
			
			if(StringUtils.equals(fId, uId)){
				logger.debug("要退出的人:{},家庭:{}",uId,fId);
				r.put(status, "0020");
				return r;
			}

			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();

			User user = loginSvr.userGet(User.UserIDColumn, uId);
			User familyUser = loginSvr.userGet(User.UserIDColumn, fId);
			String hostNick = familyUser.getNick();
			String nick = user.getNick();

			mm.put("hostId", fId);
			mm.put("hostNick", hostNick);
			mm.put("mId", uId);
			mm.put("mNick", nick);
			msg.append(JSON.toJSON(mm));

			j.hdel("user:family", uId);
			j.srem("family:"+fId, uId);
			
			Set<String>members = j.smembers("family:"+fId);
			for(String m : members){
				Set<String> devices = j.smembers("u:"+m+":devices");
				for(String o : devices){
					StringBuilder sb = new StringBuilder();
					//将uid与oper下的所有设备做关联
					sb.append(o).append("|").append(uId).append("|").append("0");
					j.publish("PubDeviceUsers", sb.toString());
				}
			}

			//用户退出家庭，除了给家庭其他成员推送外，还需要给用户自己推送消息。
			members.add(uId);

			String memlist = StringUtils.join(members.iterator(), ",") + "|";

			j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(memlist, BizCode.FamilyQuit.getValue(), Integer.parseInt(uId), msg.toString()));
			r.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		return r;
	}
}
