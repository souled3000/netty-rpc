package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0001;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0002;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0031;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

@Controller("/mobile")
public class IdentificationApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(HandlerAdapter.class);

	@Autowired
	private IUserSvr usrSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String cookie = req.getParameter("cookie");
		Jedis j = null;
		try {
			j = JedisHelper.getJedis();
			byte[] k = j.get(URLDecoder.decode(cookie, "utf8").getBytes());
			if (null == k) {
				r.put(status, C0002.toString());
				return r;
			}
			Object[] u = CookieUtil.gotUsr(cookie, k);
			String userId = (String) u[0];
			byte[] shadowMd5 = (byte[]) u[1];
			String stub = j.get("user:cookie:" + userId);
			// cookie过期了
			if (StringUtils.isBlank(stub)) {
				r.put(status, C0002.toString());
				return r;
			}

			// 一个账户只能同时在一台机器上登录
			if (!stub.equals(URLDecoder.decode(cookie, "utf8"))) {
				r.put(status, C0031.toString());
				return r;
			}

			User user = null;
			String shadow = null;
			user = usrSvr.getUser(User.UserIDColumn, userId);
			if (user == null) {
				r.put(status, C0001.toString());
				return r;
			}
			req.setUserId(userId);
			r.put("id", userId);
			shadow = user.getShadow();

			if (!CookieUtil.verifyMd5(shadowMd5, shadow)) {
				r.put(status, C0002.toString());
				logger.info("failed validating user {}", r.get(status));
				return r;
			}

			// 刷新cookie有效期
			j.expire("user:cookie:" + userId, Constants.USER_COOKIE_EXPIRE);
			j.expire(URLDecoder.decode(cookie, "utf8").getBytes(), Constants.USER_COOKIE_EXPIRE);
		} catch (Exception e) {
			logger.error("Check cookie failed", e);
			return r;
		} finally {
			JedisHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}