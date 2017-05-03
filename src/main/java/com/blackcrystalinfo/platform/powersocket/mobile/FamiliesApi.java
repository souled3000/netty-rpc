package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

import redis.clients.jedis.Jedis;

@Controller("/mobile/families")
public class FamiliesApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(FamiliesApi.class);
	@Autowired
	IUserSvr userSvr;
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String userId = req.getUserId();

		Jedis j = null;
		try {
			j = JedisHelper.getJedis();
			String fId = j.hget("user:family", userId);
			Set<String> familySet = j.smembers("family:" + fId);
			r.put("fId", fId);

			Set<Map<String, Object>> members = new HashSet<Map<String, Object>>();
			for (String uId : familySet) {
				Map<String, Object> member = new HashMap<String, Object>();
				User user = userSvr.getUser(User.UserIDColumn, uId);
				String facestamp = j.hget("user:facestamp", uId);
				member.put("uId", uId);
				member.put("nick", user.getNick()==null?user.getPhone():user.getNick());
				member.put("username", user.getUserName());
				member.put("mobile", user.getPhone());
				member.put("email", user.getEmail()==null?"":user.getEmail());
				member.put("family", fId);
				member.put("facestamp", facestamp==null?"":facestamp);
				members.add(member);
			}
			r.put("members", members);

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			r.put(status, SYSERROR.toString());
			logger.error("Get the members of a family. userId:{}|status:{}", userId, r.get(status), e);
			return r;
		} finally {
			JedisHelper.returnJedis(j);
		}
		return r;
	}

	public static void main(String[] args) {
		/*
		 * Set s = new HashSet(); s.add(1); s.add(16); s.add(32); Map m = new HashMap(); m.put("families", s); System.out.println(JSON.toJSON(s)); System.out.println(JSON.toJSON(m));
		 */
	}
}
