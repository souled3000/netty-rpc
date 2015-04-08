package com.blackcrystalinfo.platform.powersocket.api;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0008;
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
/**
 * 
 * @author juliana
 *	解绑用户
 */
@Path(path="/mobile/demission")
public class DemissionApi extends HandlerAdapter{
	private static final Logger logger = LoggerFactory.getLogger(DemissionApi.class);
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String uId = HttpUtil.getPostValue(req.getParams(), "uId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String family = CookieUtil.gotUserIdFromCookie(cookie);
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String userEmail = j.hget("user:email", uId);
			if (null == userEmail) {
				r.put(status, C0006.toString());
				return r;
			}

			long b1 = j.zrem(family + "u", uId);// u:user;用户移出家庭;<fid+'u'>家庭用户组的key
			if (b1 == 0) {
				r.put(status, C0008.toString()); // 家庭不存在此成员
				return r;
			} else {
				j.zrem(uId, family);
			}

			//获取家庭所有设备
			Set<String> devices = j.zrange(family+"d",0,-1);
			for(String o : devices){
				StringBuilder sb = new StringBuilder();
				//将uid与family下的所有设备切断关联
				sb.append(o).append("|").append(uId).append("|").append("0");
				j.publish("PubDeviceUsers", sb.toString());
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("Bind in error uId:{}|cookie:{}|status:{}", uId, family, cookie, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, SUCCESS.toString());
		return r;
	}
}
