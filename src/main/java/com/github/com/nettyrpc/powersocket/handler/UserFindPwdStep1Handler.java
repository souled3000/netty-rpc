package com.github.com.nettyrpc.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;
import com.github.com.nettyrpc.util.Constants;
import com.github.com.nettyrpc.util.HttpUtil;
import com.github.com.nettyrpc.util.VerifyCode;
import com.github.com.nettyrpc.util.mail.MailSenderInfo;
import com.github.com.nettyrpc.util.mail.SimpleMailSender;

public class UserFindPwdStep1Handler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdStep1Handler.class);
	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);
		UserFindPwdStep1Response resp = new UserFindPwdStep1Response();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());

		String destEmailAddr = HttpUtil.getPostValue(req.getParams(), "email");
		
		if(StringUtils.isBlank(destEmailAddr)){
			resp.setStatus(2);
			return resp;
		}
		
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			// 查找email
			String userId = jedis.hget("user:mailtoid", destEmailAddr);
			if (null == userId) {
				resp.setStatus(1);
				return resp;
			}

			String emailAddr = Constants.getProperty("email.user", "");
			String emailPwd = Constants.getProperty("email.pwd", "");
			String mailHost = Constants.getProperty("mail.server.host", "");
			String mailPost = Constants.getProperty("mail.server.port", "");

			// 生成验证码
			String code = VerifyCode.randString(CODE_LENGTH);
			// 保存验证码
			jedis.del(destEmailAddr + "-code");
			jedis.del(destEmailAddr + "-code"+"fail");
			jedis.setex(destEmailAddr + "-code", CODE_EXPIRE, code);
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
			mailInfo.setContent("请尽快使用此验证码重置您的密码:" + code);
			SimpleMailSender sms = new SimpleMailSender();
			boolean b = sms.sendTextMail(mailInfo);
			if(!b){
				logger.info("sending Email failed!!!");
			}
			
		} catch (Exception e) {
			resp.setStatus(-1);
			DataHelper.returnBrokenJedis(jedis);
			logger.error("UserFindPwdStep1", e);
			return resp;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		resp.setStatus(0);
		return resp;
	}

	private class UserFindPwdStep1Response extends ApiResponse {
	}
}
