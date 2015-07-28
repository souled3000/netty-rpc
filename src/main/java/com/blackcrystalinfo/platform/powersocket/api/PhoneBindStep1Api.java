package com.blackcrystalinfo.platform.powersocket.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.CookieUtil;

/**
 * 绑定手机号码的第一步，发送短信验证码。
 * 
 * @author j
 * 
 */
@Controller("/mobile/phonebindstep1")
public class PhoneBindStep1Api extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(PhoneBindStep1Api.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		// 入参解析：cookie， phone
		String cookie = req.getParameter("cookie");
		String phone = req.getParameter("phone");

		// phone是否格式正确？用户是否存在？
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		
		
		
		// 用户已经绑定手机号码？
		
		
		// 发送验证码次数是否超限？

		// 发送验证码是否成功？

		return super.rpc(req);
	}
}
