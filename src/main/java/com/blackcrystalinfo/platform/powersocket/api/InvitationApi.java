package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0007;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.HttpUtil;
@Path(path="/mobile/invitation")
public class InvitationApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(InvitationApi.class);
	public Object rpc(RpcRequest req) throws Exception {
		long l = System.currentTimeMillis();
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String family = CookieUtil.gotUserIdFromCookie(cookie);
		String uId = HttpUtil.getPostValue(req.getParams(), "uId");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String userEmail = j.hget("user:email", uId);
			if (null == userEmail) {
				r.put(status, C0006.toString());
				logger.info("There is not this user. uId:{}|family:{}|cookie:{}|status:{}", uId, family, cookie, r.get("status"));
				return r;
			}

			long b1 = j.zadd(family + "u",(double)l, uId);// u:user;用户加入家庭;<fid+'u'>家庭用户组的key
			if (b1 == 0) {
				r.put(status, C0007.toString()); // 重复绑定
				logger.info("User device has binded! uId:{}|cookie:{}|status:{}", uId, family, cookie, r.get("status"));
				return r;
			} else {
				j.zadd(uId, (double) l, family);
			}

			//获取家庭所有设备
			Set<String> devices = j.zrange(family+"d",0,-1);
			for(String o : devices){
				StringBuilder sb = new StringBuilder();
				//将uid与family下的所有设备做关联
				sb.append(o).append("|").append(uId).append("|").append("1");
				j.publish("PubDeviceUsers", sb.toString());
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("Bind in error uId:{}|cookie:{}|status:{}", uId, family, cookie, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status,SUCCESS.toString());
		return r;
	}
}
