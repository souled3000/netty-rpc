package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0018;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0019;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0020;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0021;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.net.URLEncoder;
import java.security.MessageDigest;
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
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

/**
 * 用户登陆
 * 
 * @author Shenjz
 */
@Controller("/login")
public class UserLoginApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(UserLoginApi.class);

	@Autowired
	ILoginSvr loginSvr;

	private boolean validUser(String email, String pwd,
			Map<Object, Object> mapping) {
		if (StringUtils.isBlank(pwd)) {
			mapping.put(status, C0018.toString());
			logger.debug("pwd is null when loging email:{}|pwd:{}|status:{}",
					email, pwd, mapping.get(status));
			return false;
		}
		if (StringUtils.isBlank(email)) {
			mapping.put(status, C0019.toString());
			logger.debug("email is null when loging email:{}|pwd:{}|status:{}",
					email, pwd, mapping.get(status));
			return false;
		}
		return true;
	}

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		// 1. get param from request
		String email = req.getParameter("email");
		String pwd = req.getParameter("passwd");

		logger.debug("UserLoginHandler begin email:{}|pwd:{}", email, pwd);

		// 2. valid the user
		if (!validUser(email, pwd, r)) {
			return r;
		}

		try {
			User user = loginSvr.userGet(User.UserNameColumn, email);
			email = email.toLowerCase();

			// 3. get user id
			String userId = user.getId();
			if (null == userId) {
				r.put(status, C0020.toString());
				logger.debug("User not exist. email:{}|pwd:{}|status:{}",
						email, pwd, r.get(status));
				return r;
			}

			// 4. valid password
			if (!user.validate(pwd)) {
				r.put(status, C0021.toString());
				logger.debug(
						"PBKDF2.validate Password error. email:{}|pwd:{}|status:{}",
						email, pwd, r.get(status));
				return r;
			}
			r.put("userId", userId);
			r.put("cookie", user.getCookie());

			// 5. set success
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("User login error. email:{}|pwd:{}|status:{}", email,
					pwd, r.get(status), e);
			return r;
		}
		return r;
	}

	public static void main(String[] args) throws Exception {
		String userId = "32";
		String shadow = "1000:5b42403231343135303336:8c221506a25e95e82b02720e3957c98b405b40b9c2d0454e3d8a596cb4e9c01688447c9e7582e4c35aaf8c043f608d9e06cc40b1887ede5b35228eb43cc8a3a7";
		byte[] upmd5 = MessageDigest.getInstance("MD5").digest(
				(userId + shadow).getBytes());
		System.out.println(ByteUtil.toHex(upmd5));
		System.out.println(URLEncoder.encode(
				"55E22812C947C5C7BB3E77CEE7652280", "iso-8859-1"));
		System.out.println(URLEncoder.encode("@", "iso-8859-1"));
	}
}
