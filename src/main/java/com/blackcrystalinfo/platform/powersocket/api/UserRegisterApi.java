package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0022;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0023;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0024;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0027;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.PBKDF2;
import com.blackcrystalinfo.platform.util.mail.MailSenderInfo;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;

import io.netty.handler.codec.http.HttpHeaders;
import redis.clients.jedis.Jedis;
/**
 * 用户注册
 * @author juliana
 */
@Path(path="/register")
public class UserRegisterApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserRegisterApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String email = req.getParameter( "email");
		String phone = req.getParameter( "phone");
		String pwd = req.getParameter( "passwd");
		String nick = req.getParameter( "nick");

		logger.debug("UserRegisterHandler begin email:{}|phone:{}|passwd:{}", email, phone, pwd);

		if (StringUtils.isBlank(email)) {
			r.put(status, C0022.toString());
			logger.debug("email is null. email:{}|phone:{}|passwd:{}|status:{}", email, phone, pwd, r.get(status));
			return r;
		}
		if (StringUtils.isBlank(pwd)) {
			r.put(status, C0023.toString());
			logger.debug("pwd is null. email:{}|phone:{}|passwd:{}|status:{}", email, phone, pwd, r.get(status));
			return r;
		}

		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String cookie = req.getHeaders().get(HttpHeaders.Names.COOKIE);
			String flag = j.get("B0001" + cookie);
			if(Captcha.validity)
			if (flag == null || !flag.equals("succ")) {
				r.put(status, C0027.toString());
				logger.debug("captcha fail.");
				return r;
			}
			email = email.toLowerCase();
			// 1. 邮箱是否注册
			String existId = j.hget("user:mailtoid", email);
			if (null != existId) {
				r.put(status, C0024.toString());
				logger.debug("user has been existed. email:{}|phone:{}|passwd:{}|status:{}", email, phone, pwd, r.get(status));
				return r;
			}
			String userId = String.valueOf(j.incrBy("user:nextid", 16));
			long intUserId = Long.parseLong(userId);
			if (intUserId % 16 > 0) {
				logger.info("userId can not modulo 16", userId);
				userId = String.valueOf(intUserId - intUserId % 16);
			}
			
			j.hset("user:mailtoid", email, userId);
			j.hset("user:email", userId, email);
			
			j.hset("user:phone", userId, phone);
			j.hset("user:nick", userId, nick);
			String shadow = PBKDF2.encode(pwd);
			j.hset("user:shadow", userId, shadow);
			
			// 2. 生成uuid
			
			r.put("uid", userId);
			
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
			mailInfo.setUserName(emailAddr);//source
			mailInfo.setPassword(emailPwd);//source
			mailInfo.setFromAddress(emailAddr);//source
			mailInfo.setToAddress(email);//target
			mailInfo.setSubject("用户注册确认");

			String linkAddr = protocol + "://" + ip + ":" + port + "/cfm?v=" + userId;
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
			SimpleMailSender sms = new SimpleMailSender();
			boolean b = sms.sendHtmlMail(mailInfo);
			if(!b){
				logger.info("sending Email failed!!!|{}",email);
				r.put(status, C0011.toString());
				return r;
			}
			
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("User regist error, email:{}|phone:{}|passwd:{}|status:{}", email, phone, pwd, r.get(status), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
