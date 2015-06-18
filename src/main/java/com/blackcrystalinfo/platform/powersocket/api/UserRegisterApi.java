package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0022;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0023;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0024;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0027;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;
import io.netty.handler.codec.http.HttpHeaders;

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
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.PBKDF2;
import com.blackcrystalinfo.platform.util.mail.MailSenderInfo;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;

/**
 * 用户注册
 * 
 * @author Shenjz
 */
@Controller("/register")
public class UserRegisterApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(UserRegisterApi.class);

	@Autowired
	ILoginSvr loginSvr;

	private boolean validUser(String email, String phone, String pwd,
			Map<Object, Object> mapping) {
		logger.debug("UserRegisterHandler begin email:{}|phone:{}|passwd:{}",
				email, phone, pwd);

		if (StringUtils.isBlank(email)) {
			mapping.put(status, C0022.toString());
			logger.debug(
					"email is null. email:{}|phone:{}|passwd:{}|status:{}",
					email, phone, pwd, mapping.get(status));
			return false;
		}
		if (StringUtils.isBlank(pwd)) {
			mapping.put(status, C0023.toString());
			logger.debug("pwd is null. email:{}|phone:{}|passwd:{}|status:{}",
					email, phone, pwd, mapping.get(status));
			return false;
		}

		return true;
	}

	private boolean checkCookie(Jedis j, String cookie,
			Map<Object, Object> mapping) {
		String flag = j.get("B0001" + cookie);
		if (Captcha.validity)
			if (flag == null || !flag.equals("succ")) {
				mapping.put(status, C0027.toString());
				logger.debug("captcha fail.");
				return false;
			}
		return true;
	}

	private boolean sendEmail(String userId, String email,
			Map<Object, Object> mapping) {
		String emailAddr = Constants.getProperty("email.user", "");
		String emailPwd = Constants.getProperty("email.pwd", "");
		String mailHost = Constants.getProperty("mail.server.host", "");
		String mailPost = Constants.getProperty("mail.server.port", "");

		String protocol = Constants.SERVERPROTOCOL;
		String ip = Constants.SERVERIP;
		String port = Constants.SERVERPORT;

		MailSenderInfo mailInfo = new MailSenderInfo();
		mailInfo.setMailServerHost(mailHost);
		mailInfo.setMailServerPort(mailPost);
		mailInfo.setValidate(true);
		mailInfo.setUserName(emailAddr);// source
		mailInfo.setPassword(emailPwd);// source
		mailInfo.setFromAddress(emailAddr);// source
		mailInfo.setToAddress(email);// target
		mailInfo.setSubject("用户注册确认");

		String linkAddr = protocol + "://" + ip + ":" + port + "/cfm?v="
				+ userId;
		StringBuilder sb = new StringBuilder();
		sb.append("点击如下链接马上完成邮箱验证：");
		sb.append("<br>");
		sb.append("<a href='" + linkAddr + "'>激活</a>");
		sb.append("<br>");
		sb.append("<br>");
		sb.append("如果链接无法点击，请完整拷贝到浏览器地址栏里直接访问，链接如下：");
		sb.append("<br>");
		sb.append(linkAddr);

		mailInfo.setContent(sb.toString());
		boolean b = SimpleMailSender.sendHtmlMail(mailInfo);
		if (!b) {
			logger.info("sending Email failed!!!|{}", email);
			mapping.put(status, C0011.toString());
			return false;
		}
		return true;
	}

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		// 1. get the params from request
		String email = req.getParameter("email");
		String phone = req.getParameter("phone");
		String pwd = req.getParameter("passwd");
		String nick = req.getParameter("nick");

		// 2. valid the params
		if (!validUser(email, phone, pwd, r)) {
			return r;
		}

		Jedis j = null;
		try {
			// 3. get & check the cookie
			j = DataHelper.getJedis();
			String cookie = req.getHeaders().get(HttpHeaders.Names.COOKIE);
			if (!checkCookie(j, cookie, r)) {
				return r;
			}

			// 4. check whether user exist
			email = email.toLowerCase();
			boolean exist = loginSvr.userExist(email);

			if (exist) {
				r.put(status, C0024.toString());
				logger.debug(
						"user has been existed. email:{}|phone:{}|passwd:{}|status:{}",
						email, phone, pwd, r.get(status));
				return r;
			}

			// 5. register the user
			loginSvr.userRegister(email, phone, nick, PBKDF2.encode(pwd));

			// 6. get the user id
			String userId = loginSvr.userGet(User.UserNameColumn, email)
					.getId();
			r.put("uid", userId);

			// 7. send the email for register
			if (!sendEmail(userId, email, r)) {
				return r;
			}

			// 8. set success code
			r.put(status, SUCCESS.toString());

		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			DataHelper.returnBrokenJedis(j);
			logger.error(
					"User regist error, email:{}|phone:{}|passwd:{}|status:{}",
					email, phone, pwd, r.get(status), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		return r;
	}
}
