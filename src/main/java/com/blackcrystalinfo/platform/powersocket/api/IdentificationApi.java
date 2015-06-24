package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0001;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0002;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.CookieUtil;

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

		try {
			String userId = CookieUtil.gotUserIdFromCookie(cookie);
			User user = loginSvr.userGet(User.UserIDColumn, userId);
			String email = user.getEmail();
			if (null == email) {
				r.put(status, C0001.toString());
				logger.info("failed validating user {}", r.get(status));
				return r;
			}

			String shadow = user.getShadow();
			if (!CookieUtil.validateMobileCookie(cookie, shadow)) {
				r.put(status, C0002.toString());
				logger.info("failed validating user {}", r.get(status));
				return r;
			}
		} catch (Exception e) {
			logger.error("Check cookie failed, e = ", e);
			return r;
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
