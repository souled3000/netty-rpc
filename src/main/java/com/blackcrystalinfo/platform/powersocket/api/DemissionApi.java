package com.blackcrystalinfo.platform.powersocket.api;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
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
import com.blackcrystalinfo.platform.util.Utils;

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
		String userId = req.getParameter( "uId");
		String family = CookieUtil.gotUserIdFromCookie(req.getParameter( "cookie"));
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String userEmail = j.hget("user:email", userId);
			if (null == userEmail) {
				r.put(status, C0006.toString());
				return r;
			}

			j.hdel("user:family", userId);
			j.srem("family:"+family, userId);
			
			Set<String>members = j.smembers("family:"+family);
			for(String m : members){
				Set<String> devices = j.smembers("u:"+m+":devices");
				for(String o : devices){
					StringBuilder sb = new StringBuilder();
					//将uid与oper下的所有设备做关联
					sb.append(o).append("|").append(userId).append("|").append("0");
					j.publish("PubDeviceUsers", sb.toString());
				}
			}
			j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(userId+"|",2, Integer.parseInt(userId), ""));
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("Bind in error uId:{}|family:{}|status:{}", userId, family, family, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, SUCCESS.toString());
		return r;
	}
}
