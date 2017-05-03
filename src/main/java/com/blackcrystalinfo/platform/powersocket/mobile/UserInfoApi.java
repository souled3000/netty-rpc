package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.service.IUserSvr;

/**
 * 获取用户信息<br>
 * 
 * 用户登录后获取用户信息接口
 * 
 * @author shenjizhe
 * 
 */

@Controller("/mobile/usr")
public class UserInfoApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserInfoApi.class);

	@Autowired
	IUserSvr loginSvr;

	@Override
	public Object rpc(com.blackcrystalinfo.platform.server.RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String userId = req.getParameter("uId");
		Jedis jedis = null;

		try {
			jedis = JedisHelper.getJedis();
			User user = loginSvr.getUser(User.UserIDColumn, userId);

			String family = jedis.hget("user:family", userId);
			user.setAdminid(family);

			r.put("uId", userId);
			r.put("nick", user.getNick()==null?user.getPhone():user.getNick());
			r.put("mobile", user.getPhone());
			r.put("family", user.getAdminid());

			// 头像的md5值，手机端通过该值判断
			String facestamp = jedis.hget("user:facestamp", userId);
			r.put("facestamp", facestamp==null?"":facestamp);

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("User Info API", e);
			return r;
		}finally {
			JedisHelper.returnJedis(jedis);
		}

		return r;
	};
}
