package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

/**
 * 获取用户信息<br>
 * 
 * 用户登录后获取用户信息接口
 * 
 * @author j
 *
 */

@Path(path = "/mobile/getUserInfo")
public class UserInfoApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(UserInfoApi.class);

	public Object rpc(com.blackcrystalinfo.platform.RpcRequest req)
			throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String cookie = req.getParameter("cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String nick = jedis.hget("user:nick", userId);
			String email = jedis.hget("user:email", userId);
			String mobile = jedis.hget("user:phone", userId);

			r.put("nick", nick);
			r.put("email", email);
			r.put("mobile", mobile);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("", e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		r.put(status, SUCCESS.toString());
		return r;
	};
}
