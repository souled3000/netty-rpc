package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C001E;
import static com.blackcrystalinfo.platform.util.ErrorCode.C001F;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

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
import com.blackcrystalinfo.platform.util.Utils;

/**
 * 邀请家庭
 * 
 * @author Shenjz
 */
@Controller("/mobile/invitation")
public class InvitationApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(InvitationApi.class);
	
	@Autowired
	ILoginSvr loginSvr;
	
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		
		String oper = CookieUtil.gotUserIdFromCookie(req.getParameter("cookie"));
		String uId = req.getParameter("uId");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			User user = loginSvr.userGet(User.UserIDColumn, uId);
			if (null == user) {
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
				r.put(status,C001E.toString());
				return r;
			}
			
			if(StringUtils.isBlank(uFamily)){
				
			}else{
				r.put(status, C001F.toString());
				return r;
			}
			
			User operator = loginSvr.userGet(User.UserIDColumn, oper);
			User familyUser = loginSvr.userGet(User.UserIDColumn, uId);
			
			String mnick = operator.getNick();
			String nick = familyUser.getNick();
			StringBuilder msg = new StringBuilder();
			Map<String,String> mm = new HashMap<String,String>();
			mm.put("hostId", oper);
			mm.put("hostNick", mnick);
			mm.put("mId", uId);
			mm.put("mNick", nick);
			msg.append(JSON.toJSON(mm));
			j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(uId+"|",BizCode.FamilyInvite.getValue(), Integer.parseInt(uId), msg.toString()));
			
			r.put(status,SUCCESS.toString());
		} catch (Exception e) {
			//DataHelper.returnBrokenJedis(j);
			r.put(status, SYSERROR.toString());
			logger.error("Bind in error uId:{}|status:{}", uId, oper, r.get("status"), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		
		return r;
	}
}
