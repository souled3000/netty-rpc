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
import com.blackcrystalinfo.platform.util.DataHelper;

/**
 * 获取指定用户信息<br>
 * 
 * 获取家庭成员的用户基本信息：包括昵称，已经激活的邮件
 * 
 * @author j
 *
 */

@Path(path = "/getUserInfo")
public class UserInfoApi2 extends HandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(UserInfoApi2.class);

	public Object rpc(com.blackcrystalinfo.platform.RpcRequest req)
			throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String userId = req.getParameter("uid");

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String nick = jedis.hget("user:nick", userId);
			String email = jedis.hget("user:email", userId);
			r.put("nick", nick);
			r.put("email", email);
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
