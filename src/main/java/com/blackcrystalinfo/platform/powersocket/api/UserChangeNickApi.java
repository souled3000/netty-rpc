package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0025;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0026;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
 * 修改用户昵称
 * 
 * @author j
 * 
 */
@Path(path="/mobile/cn")
public class UserChangeNickApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserChangeNickApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());

		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		String nick = HttpUtil.getPostValue(req.getParams(), "nick");
		
		logger.info("UserChangeNickHandler begin userId:{}|cookie:{}|nick:{}", userId, cookie, nick);

		if(StringUtils.isBlank(nick)){
			r.put(status, C0025.toString());
			return r;
		}
		
		Jedis jedis = null;
		// 1. 校验cookie信息
		try {
			jedis = DataHelper.getJedis();
			
			String oldNick = jedis.hget("user:nick", userId);
			if (!nick.equals(oldNick)) {
				// 新旧Nick不一致时修改
				jedis.hset("user:nick", userId, nick);
			}else{
				r.put(status, C0026.toString());
				return r;
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("", e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		logger.info("response: userId:{}|cookie:{}|nick:{}|status:{}", userId, cookie, nick, r.get(status));
		r.put(status,SUCCESS.toString());
		return r;
	}

}
