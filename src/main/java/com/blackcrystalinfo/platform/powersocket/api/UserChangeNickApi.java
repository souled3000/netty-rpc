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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.CookieUtil;

/**
 * 修改用户昵称
 * 
 * @author shenjizhe
 * 
 */
@Controller("/mobile/cn")
public class UserChangeNickApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(UserChangeNickApi.class);

	@Autowired
	ILoginSvr loginSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String cookie = req.getParameter("cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		String nick = req.getParameter("nick");

		logger.info("UserChangeNickHandler begin userId:{}|cookie:{}|nick:{}",
				userId, cookie, nick);

		if (StringUtils.isBlank(nick)) {
			r.put(status, C0025.toString());
			return r;
		}

		// 1. 校验cookie信息
		try {
			User user = loginSvr.userGet(User.UserIDColumn, userId);
			if (!nick.equals(user.getNick())) {
				// 新旧Nick不一致时修改
				loginSvr.userChangeProperty(userId, User.UserNickColumn,nick);
			} else {
				r.put(status, C0026.toString());
				return r;
			}
			r.put(status, SUCCESS.toString());
			logger.info("response: userId:{}|cookie:{}|nick:{}|status:{}",
					userId, cookie, nick, r.get(status));
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("", e);
			return r;
		}
		return r;
	}

}
