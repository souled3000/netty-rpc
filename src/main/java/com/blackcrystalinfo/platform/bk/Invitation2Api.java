package com.blackcrystalinfo.platform.bk;

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
@Path(path="/mobile/invitation")
public class Invitation2Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(Invitation2Api.class);
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());
		String cookie = req.getParameter("cookie");
		String oper = CookieUtil.gotUserIdFromCookie(cookie);
		String uId = req.getParameter("uId");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String userEmail = j.hget("user:email", uId);
			if (null == userEmail) {
				r.put(status, C0006.toString());
				logger.info("There is not this user. uId:{}|oper:{}|status:{}", uId, oper, r.get("status"));
				return r;
			}

			//判断oper与uid是否为主
			//如果oper是主或，oper与uid都不为主，可以进行邀请操作
			//如果oper不是主，而oper是主，则不允许邀请操作
			String operFamily = j.hget("user:family", oper);
			String uFamily = j.hget("user:family", uId);
			
			//操作员是另的家庭的成员，不具有添加成员的权限
			if(StringUtils.isNotBlank(operFamily)&&!StringUtils.equals(operFamily, oper)){
				r.put(status,"001E");
				return r;
			}
			
			if(StringUtils.isBlank(uFamily)){
				if(StringUtils.isBlank(operFamily)){
					j.hset("user:family", oper, oper);
					j.sadd("family:"+oper, oper);
				}
				j.hset("user:family", uId, oper);
				j.sadd("family:"+oper, uId);
			}else{
				r.put(status, "001F");
				return r;
			}
			
			//获取家庭所有设备
			Set<String>members = j.smembers("family:"+oper);
			for(String m : members){
				Set<String> devices = j.smembers("u:"+m+":devices");
				for(String o : devices){
					StringBuilder sb = new StringBuilder();
					//将uid与oper下的所有设备做关联
					sb.append(o).append("|").append(uId).append("|").append("1");
					j.publish("PubDeviceUsers", sb.toString());
				}
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("Bind in error uId:{}|status:{}", uId, oper, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status,SUCCESS.toString());
		return r;
	}
}
