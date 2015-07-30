package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;

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

@Controller("/mobile/phonebindinfo")
public class PhoneBindInfoApi extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(PhoneBindInfoApi.class);

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("status", SYSERROR.toString());

		// 入参解析：cookie， phone
		String cookie = req.getParameter("cookie");

		// phone是否格式正确？用户是否存在？
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		User user = null;
		try {
			user = userDao.userGet(User.UserIDColumn, userId);

			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by id.", e);
			ret.put("status", "用户没找到");
			return ret;
		}

		// 用户已经绑定手机号
		String phone = user.getPhone();
		ret.put("phone", phone);

		String phoneable = user.getPhoneable();
		ret.put("phoneable", "true".equalsIgnoreCase(phoneable));

		ret.put("status", SUCCESS.toString());
		return ret;
	}
}
