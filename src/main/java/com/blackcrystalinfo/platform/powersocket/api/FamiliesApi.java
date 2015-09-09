package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Controller("/mobile/families")
public class FamiliesApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(FamiliesApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String userId = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));

		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String fId = j.hget("user:family", userId);
			Set<String> familySet = j.smembers("family:" + fId);
			r.put("fId", fId);
			r.put("families", familySet);

			// TODO: 需要跟手机协商，如果这个地方把成员的详细信息获取到，就不用挨个获取了
			Set<Map<String, Object>> members = new HashSet<Map<String, Object>>();
			for (String uId : familySet) {
				Map<String, Object> member = new HashMap<String, Object>();

				String nick = "";
				String username = "";
				String mobile = "";
				String email = "";
				String family = "";
				String facestamp = j.hget("user:facestamp", uId);

				member.put("uId", uId);
				member.put("nick", nick);
				member.put("username", username);
				member.put("mobile", mobile);
				member.put("email", email);
				member.put("family", family);
				member.put("facestamp", facestamp);
			}
			r.put("members", members);

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			r.put(status, SYSERROR.toString());
			logger.error("Get the members of a family. userId:{}|status:{}", userId, r.get(status), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		return r;
	}

	public static void main(String[] args) {
		/*
		 * Set s = new HashSet(); s.add(1); s.add(16); s.add(32); Map m = new HashMap(); m.put("families", s); System.out.println(JSON.toJSON(s)); System.out.println(JSON.toJSON(m));
		 */
	}
}
