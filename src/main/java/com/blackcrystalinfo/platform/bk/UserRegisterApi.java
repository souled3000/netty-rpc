package com.blackcrystalinfo.platform.bk;

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
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;

import com.blackcrystalinfo.platform.util.PBKDF2;
import com.blackcrystalinfo.platform.util.mail.MailSenderInfo;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;
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

			// 2. 生成uuid

			String uuid = UUID.randomUUID().toString();
			
			r.put("uId", uuid);
//			Transaction tx = j.multi();

			// 3. 记录<邮箱，用户Id>
//			tx.hset("user:mailtoid", email, userId);
			j.setex(uuid+"email", Constants.USRCFMEXPIRE, email);

			// 4. 记录<用户Id，邮箱>
//			tx.hset("user:email", userId, email);

			// 5. 记录<用户Id，电话号码>
			if (StringUtils.isNotBlank(phone))
//				tx.hset("user:phone", userId, phone);
			j.setex(uuid+"phone", Constants.USRCFMEXPIRE, phone);

			// 6. 记录<用户Id，密码>
			String shadow = PBKDF2.encode(pwd);
//			tx.hset("user:shadow", userId, shadow);
			j.setex(uuid+"shadow", Constants.USRCFMEXPIRE, shadow);
			
			if(StringUtils.isNotBlank(nick))
			j.setex(uuid+"nick", Constants.USRCFMEXPIRE, nick);
			
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
			mailInfo.setContent("<a href='"+protocol+ "//"+ip+":"+port+"/cfm?v=" + uuid+"'>激活</a>");
			boolean b = SimpleMailSender.sendHtmlMail(mailInfo);
			if(!b){
				logger.info("sending Email failed!!!|{}",email);
				r.put(status, C0011.toString());
				return r;
			}
			
		} catch (Exception e) {
			//DataHelper.returnBrokenJedis(j);
			logger.error("User regist error, email:{}|phone:{}|passwd:{}|status:{}", email, phone, pwd, r.get(status), e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
