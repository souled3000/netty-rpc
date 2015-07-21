package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0018;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0019;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0020;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0021;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002E;
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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.DateUtils;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

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

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 3. get user id
			User user = null;
			String userId = "";
			try{
				user = loginSvr.userGet(User.UserNameColumn, email);
				userId = user.getId();
			} catch (Exception e) {
				r.put(status, C0020.toString());
				logger.debug("User not exist. email:{}|pwd:{}|status:{}",
						email, pwd, r.get(status));
				return r;
			}
			email = email.toLowerCase();

			// 4. 获取登录失败次数
			int times = -1;
			String strTimes = jedis.get("user:failedLoginTimes:" + userId);
			if (StringUtils.isNotBlank(strTimes)) {
				times = Integer.valueOf(strTimes);
			}

			// 5. 根据失败次数，决定是否锁定账户
			if (times >= Constants.FAILED_LOGIN_TIMES_MAX) {
				Long ttl = jedis.ttl("user:failedLoginTimes:" + userId);
				r.put(status, C002E.toString());
				r.put("ttl", ttl);
				logger.debug("Accout is locked. email:{}|pwd:{}|status:{}",
						email, pwd, r.get(status));
				return r;
			}

			// 6. valid password
			if (!user.validate(pwd)) {
				times++;
				r.put(status, C0021.toString());
				r.put("leftLoginTimes", Constants.FAILED_LOGIN_TIMES_MAX
						- times);
				logger.debug(
						"PBKDF2.validate Password error. email:{}|pwd:{}|status:{}",
						email, pwd, r.get(status));

				// 判断失败次数累加
				jedis.setex("user:failedLoginTimes:" + userId,
						Constants.FAILED_LOGIN_EXPIRE, String.valueOf(times));

				// 最后一次登录失败，发送邮件通知用户
				if (times >= Constants.FAILED_LOGIN_TIMES_MAX) {
					String subject = "账户被锁定通知";
					StringBuilder sb = new StringBuilder();
					sb.append("您的帐户登录失败次数超过");
					sb.append("<b>" + Constants.FAILED_LOGIN_TIMES_MAX
							+ "次。</b>");
					sb.append("请您" + DateUtils.secToTime(Constants.FAILED_LOGIN_EXPIRE) + "秒 后重新登录");
					SimpleMailSender
							.sendHtmlMail(email, subject, sb.toString());
					
					// 发送短信
					String phone = user.getPhone();
					if (StringUtils.isNotEmpty(phone)) {
						SMSSender.send(phone, sb.toString());
					}
				}
				return r;
			}

			// 7. 登录成功后，清除累计失败次数的计数器
			jedis.del("user:failedLoginTimes:" + userId);

			// 8. generate cookie
			String cookie = user.getCookie();
			jedis.set("user:cookie:" + userId, cookie); // 用户Id->cookie映射

			r.put("userId", userId);
			r.put("cookie", cookie);

			// 6. set success
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
