package com.blackcrystalinfo.platform.powersocket.handler;

import java.net.URLEncoder;
import java.security.MessageDigest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserLoginResponse;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.PBKDF2;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class UserLoginHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(UserLoginHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		UserLoginResponse resp = new UserLoginResponse();
		resp.setStatus(-1);
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());

		String email = HttpUtil.getPostValue(req.getParams(), "email");
		String pwd = HttpUtil.getPostValue(req.getParams(), "passwd");
		
		logger.info("UserLoginHandler begin email:{}|pwd:{}",email,pwd);
		
		if(StringUtils.isBlank(pwd)){
			resp.setStatus(3);
			logger.info("pwd is null email:{}|pwd:{}|status:{}",email,pwd,resp.getStatus());
			return resp;
		}
		
		Jedis jedis = null;
		
		try {
			jedis = DataHelper.getJedis();
			email=email.toLowerCase();
			// 1. 根据Email获取userId
			String userId = jedis.hget("user:mailtoid", email);
			if (null == userId) {
				resp.setStatus(1);
				logger.info("User not exist. email:{}|pwd:{}|status:{}",email,pwd,resp.getStatus());
				return resp;
			}

			// 2. encodePwd与passwd加密后的串做比较
			String shadow = jedis.hget("user:shadow", userId);
			if (!PBKDF2.validate(pwd, shadow)) {
				resp.setStatus(2);
				logger.info("PBKDF2.validate Password error. email:{}|pwd:{}|status:{}",email,pwd,resp.getStatus());
				return resp;
			}
			
			
			String cookie = CookieUtil.encode(userId, CookieUtil.EXPIRE_SEC);
			
			String up = ByteUtil.toHex(MessageDigest.getInstance("MD5").digest((userId+shadow).getBytes()));
//			String proxyKey = CookieUtil.generateKey(userId, String.valueOf(System.currentTimeMillis()/1000), CookieUtil.EXPIRE_SEC);
//			String proxyAddr = CometScanner.take();
//			logger.info("proxykey:{} | size:{} | proxyAddr:{} ", proxyKey, proxyKey.getBytes().length, proxyAddr);
			resp.setStatus(0);
			resp.setUserId(userId);
//			result.setHeartBeat(CookieUtil.EXPIRE_SEC);
			StringBuilder sb = new StringBuilder(cookie+"-"+up);
			resp.setCookie(sb.toString());
//			result.setProxyKey(proxyKey);
//			result.setProxyAddr(proxyAddr);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("User login error. email:{}|pwd:{}|status:{}",email,pwd,resp.getStatus(),e);
			return resp;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: email:{}|pwd:{}|status:{}",email,pwd,resp.getStatus());
		return resp;
	}

	public static void main(String[] args) throws Exception{
		String userId= "32";
		String shadow = "1000:5b42403231343135303336:8c221506a25e95e82b02720e3957c98b405b40b9c2d0454e3d8a596cb4e9c01688447c9e7582e4c35aaf8c043f608d9e06cc40b1887ede5b35228eb43cc8a3a7";
		byte[] upmd5 = MessageDigest.getInstance("MD5").digest((userId+shadow).getBytes());
		System.out.println(ByteUtil.toHex(upmd5));
		System.out.println(URLEncoder.encode("55E22812C947C5C7BB3E77CEE7652280","iso-8859-1"));
		System.out.println(URLEncoder.encode("@","iso-8859-1"));
		
	}
}
