package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0018;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0019;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0021;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002E;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

import redis.clients.jedis.Jedis;

/**
 * 用户登陆
 * 
 * @author Shenjz
 */
@Controller("/login")
public class UserLoginApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserLoginApi.class);

	@Autowired
	IUserSvr loginSvr;

	private boolean validUser(String phone, String pwd, Map<Object, Object> mapping) {
		if (StringUtils.isBlank(pwd)) {
			mapping.put(status, C0018.toString());
			logger.debug("pwd is null when loging email:{}|pwd:{}|status:{}", phone, pwd, mapping.get(status));
			return false;
		}
		if (StringUtils.isBlank(phone)) {
			mapping.put(status, C0019.toString());
			logger.debug("email is null when loging email:{}|pwd:{}|status:{}", phone, pwd, mapping.get(status));
			return false;
		}
		return true;
	}

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		// 1. get param from request
		String phone = req.getParameter("phone");
		String pwd = req.getParameter("passwd");

		// 2. valid the user
		if (!validUser(phone, pwd, r)) {
			return r;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 3. get user id
			User user = null;
			String userId = "";
			user = loginSvr.getUser(User.UserPhoneColumn, phone);
			if (null == user) {
				r.put(status, ErrorCode.C0006.toString());
				return r;
			}
			userId = user.getId();
			phone = phone.toLowerCase();

			// 4. 获取登录失败次数 ftl:fail to login
			long times = jedis.incrBy("usr:ftl:" + userId,0);

			// 5. 根据失败次数，决定是否锁定账户
			if (times >= Constants.FAILED_LOGIN_TIMES_MAX) {
				Long ttl = jedis.ttl("usr:ftl:" + userId);
				r.put(status, C002E.toString());
				r.put("ttl", ttl);
				return r;
			}

			// 6. valid password
			if (!user.validate(pwd)) {
				times++;
				r.put(status, C0021.toString());
				r.put("leftLoginTimes", Constants.FAILED_LOGIN_TIMES_MAX - times);

				// 判断失败次数累加
				jedis.setex("usr:ftl:" + userId, Constants.FAILED_LOGIN_EXPIRE, String.valueOf(times));

				// // 最后一次登录失败，发送邮件通知用户
				// if (times >= Constants.FAILED_LOGIN_TIMES_MAX) {
				// String subject = "账户被锁定通知";
				// StringBuilder sb = new StringBuilder();
				// sb.append("您的帐户登录失败次数超过");
				// sb.append("<b>" + Constants.FAILED_LOGIN_TIMES_MAX + "次。</b>");
				// sb.append("请您" + DateUtils.secToTime(Constants.FAILED_LOGIN_EXPIRE) + " 后重新登录");
				// SimpleMailSender.sendHtmlMail(phone, subject, sb.toString());
				//
				// // 发送短信
				// String phone = user.getPhone();
				// if (StringUtils.isNotEmpty(phone)) {
				// SMSSender.send(phone, sb.toString());
				// }
				// }
				return r;
			}

			// 7. 登录成功后，清除累计失败次数的计数器
			jedis.del("usr:ftl:" + userId);

			// 8. generate cookie
			String cookie = CookieUtil.genUsrCki(user.getId(), user.getShadow());
			jedis.setex("user:cookie:" + userId, Constants.USER_COOKIE_EXPIRE, cookie); // 用户Id->cookie映射

			r.put("userId", userId);
			r.put("cookie", cookie);

			// 6. set success
			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("User login error. email:{}|pwd:{}|status:{}", phone, pwd, r.get(status), e);
			return r;
		}
		return r;
	}

	public static void main(String[] args) throws Exception {
		String userId = "32";
		String shadow = "1000:5b42403231343135303336:8c221506a25e95e82b02720e3957c98b405b40b9c2d0454e3d8a596cb4e9c01688447c9e7582e4c35aaf8c043f608d9e06cc40b1887ede5b35228eb43cc8a3a7";
		byte[] upmd5 = MessageDigest.getInstance("MD5").digest((userId + shadow).getBytes());
		System.out.println(ByteUtil.toHex(upmd5));
		System.out.println(URLEncoder.encode("55E22812C947C5C7BB3E77CEE7652280", "iso-8859-1"));
		System.out.println(URLEncoder.encode("@", "iso-8859-1"));
	}
}
