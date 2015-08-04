package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0001;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0002;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0031;
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

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Controller("/mobile")
public class IdentificationApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(HandlerAdapter.class);

	@Autowired
	private ILoginSvr loginSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String cookie = req.getParameter("cookie");

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			String userId = CookieUtil.gotUserIdFromCookie(cookie);
			String cookieNew = jedis.get("user:cookie:" + userId);

			// cookie过期了
			if (StringUtils.isBlank(cookieNew)) {
				r.put(status, C0002.toString());
				logger.info("the cookie is expire");
				return r;
			}

			// 一个账户只能同时在一台机器上登录
			if (!cookie.equals(cookieNew)) {
				r.put(status, C0031.toString());
				logger.info(
						"cookie is older then stored, userid={} cookie={}, cookieNew={}",
						userId, cookie, cookieNew);
				return r;
			}

			User user = null;
			String shadow = null;
			try {
				user = loginSvr.userGet(User.UserIDColumn, userId);
				shadow = user.getShadow();
			} catch (Exception e) {
				r.put(status, C0001.toString());
				logger.info("failed validating user {}", r.get(status));
				return r;
			}

			if (!CookieUtil.validateMobileCookie(cookie, shadow)) {
				r.put(status, C0002.toString());
				logger.info("failed validating user {}", r.get(status));
				return r;
			}

			// 刷新cookie有效期
			jedis.expire("user:cookie:" + userId, Constants.USER_COOKIE_EXPIRE);
		} catch (Exception e) {
			logger.error("Check cookie failed, e = ", e);
			return r;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
