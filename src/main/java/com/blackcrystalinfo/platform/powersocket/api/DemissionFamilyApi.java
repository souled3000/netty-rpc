package com.blackcrystalinfo.platform.powersocket.api;

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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.Utils;

/**
 * 
 * @author juliana 解绑用户
 */
@Path(path = "/mobile/demissionFamily")
public class DemissionFamilyApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(DemissionFamilyApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String userId = CookieUtil.gotUserIdFromCookie(req
				.getParameter("cookie"));
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String userEmail = j.hget("user:email", userId);
			if (null == userEmail) {
				r.put(status, C0006.toString());
				return r;
			}

			Set<String> members = j.smembers("family:" + userId);
			for (String m : members) {
				// 删除由用户找家庭的关系表-》user:family
				j.hdel("user:family", m);

				// 使用户不可以控制设备
				Set<String> devices = j.smembers("u:" + m + ":devices");
				for (String o : devices) {
					StringBuilder sb = new StringBuilder();
					// 将uid与oper下的所有设备做关联
					sb.append(o).append("|").append(m).append("|").append("0");
					j.publish("PubDeviceUsers", sb.toString());
				}
			}

			// 删除户主
			j.hdel("user:family", userId);

			// 删除家庭下所有用户的关系表-》family:${family}
			j.del("family:" + userId);
			String memlist = StringUtils.join(members.iterator(), ",") + "|";
			j.publish("PubCommonMsg:0x36".getBytes(),
					Utils.genMsg(memlist, 4, Integer.parseInt(userId), ""));
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("DemissionFamily error uId:{}|status:{}|msg:{}",
					userId, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, SUCCESS.toString());
		return r;
	}
}
