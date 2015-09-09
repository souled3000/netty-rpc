package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.DataHelper;

/**
 * 获取用户信息<br>
 * 
 * 用户登录后获取用户信息接口
 * 
 * @author shenjizhe
 * 
 */

@Controller("/mobile/getUserInfo")
public class UserInfoApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserInfoApi.class);

	@Autowired
	ILoginSvr loginSvr;

	@Override
	public Object rpc(com.blackcrystalinfo.platform.RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		String userId = req.getParameter("uId");
		Jedis jedis = null;

		try {
			jedis = DataHelper.getJedis();
			User user = loginSvr.userGet(User.UserIDColumn, userId);

			String family = jedis.hget("user:family", userId);
			user.setAdminid(family);

			r.put("uId", userId);
			r.put("nick", user.getNick());
			r.put("username", user.getUserName());
			r.put("mobile", user.getPhone());
			r.put("email", user.getAbleEmail());
			r.put("family", user.getAdminid());

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("User Info API", e);
			return r;
		}

		return r;
	};
}
