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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.Utils;

@Path(path="/mobile/invitationcfm")
public class InvitationCfmApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(InvitationCfmApi.class);
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());
		
		String uId = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));
		String oper = req.getParameter("uId");
		String asw = req.getParameter("asw");
		Jedis j = null;
		try{
			j= DataHelper.getJedis();
			
			if("yes".equals(asw)){
				
				String operFamily = j.hget("user:family", oper);
				
				if(StringUtils.isBlank(operFamily)){
					j.hset("user:family", oper, oper);
				}
				j.hset("user:family", uId, oper);
				j.sadd("family:"+oper, oper);
				j.sadd("family:"+oper, uId);
				
				
				
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
				String memlist = StringUtils.join(members.iterator(), ",")+"|";
				
				j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(memlist, 1, Long.parseLong(uId), ""));
			}else{
				j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(String.valueOf(oper)+"|", 1, Long.parseLong(uId), ""));
				
			}
		}catch(Exception e){
			logger.error("",e);
			DataHelper.returnBrokenJedis(j);
		}finally{
			DataHelper.returnJedis(j);
		}
		
		r.put(status,SUCCESS.toString());
		return r;
	}
}
