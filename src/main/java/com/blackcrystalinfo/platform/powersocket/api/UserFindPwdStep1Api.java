package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0010;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.DateUtils;
import com.blackcrystalinfo.platform.util.VerifyCode;
import com.blackcrystalinfo.platform.util.mail.MailSenderInfo;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;
@Path(path="/step1")
public class UserFindPwdStep1Api extends HandlerAdapter  {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdStep1Api.class);
	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());
		
//		String cookie = req.getParameter( "cookie");
//		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		String destEmailAddr = req.getParameter( "email");
		logger.info("UserFindPwdStep1Handler: email:{}", destEmailAddr);
		
		if(StringUtils.isBlank(destEmailAddr)){
			r.put(status, C0010.toString());
			logger.info("email is null : email:{}|status:{}", destEmailAddr,r.get(status));
			return r;
		}
		
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			
			String cookie = req.getHeaders().get(HttpHeaders.Names.COOKIE);
			String flag = j.get("B0012" + cookie);
			
			if(Captcha.validity)
			if (flag == null || !flag.equals("succ")) {
				r.put(status, C0027.toString());
				logger.debug("captcha fail.");
				return r;
			}
			
			// 查找email
			String userId = j.hget("user:mailtoid", destEmailAddr);
			if (null == userId) {
				r.put(status, C0006.toString());
				return r;
			}

			String emailAddr = Constants.getProperty("email.user", "");
			String emailPwd = Constants.getProperty("email.pwd", "");
			String mailHost = Constants.getProperty("mail.server.host", "");
			String mailPost = Constants.getProperty("mail.server.port", "");

			// 生成验证码
			String code = VerifyCode.randString(CODE_LENGTH);
			// 保存验证码
			j.del(destEmailAddr + "-code");
			j.del(destEmailAddr + "-code"+"fail");
			j.setex(destEmailAddr + "-code", CODE_EXPIRE, code);
			// 发送到邮箱
			MailSenderInfo mailInfo = new MailSenderInfo();
			mailInfo.setMailServerHost(mailHost);
			mailInfo.setMailServerPort(mailPost);
			mailInfo.setValidate(true);
			mailInfo.setUserName(emailAddr);
			mailInfo.setPassword(emailPwd);// 您的邮箱密码
			mailInfo.setFromAddress(emailAddr);
			mailInfo.setToAddress(destEmailAddr);
			mailInfo.setSubject("验证码邮件");
			mailInfo.setContent("请尽快使用此验证码重置您的密码：" + code 
					+ " 验证超时时间：" + DateUtils.secToTime(CODE_EXPIRE));
			boolean b = SimpleMailSender.sendTextMail(mailInfo);
			if(!b){
				logger.info("sending Email failed!!!");
				r.put(status, C0011.toString());
				return r;
			}
			
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("UserFindPwdStep1", e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		logger.info("response: email:{}|status:{}", destEmailAddr,r.get(status));
		r.put(status,SUCCESS.toString());
		return r;
	}

}
