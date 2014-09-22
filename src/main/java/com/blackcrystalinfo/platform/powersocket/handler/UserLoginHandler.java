package com.blackcrystalinfo.platform.powersocket.handler;

import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserLoginResponse;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.PBKDF2;

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
			String encodePwd = jedis.hget("user:shadow", userId);
			if (!PBKDF2.validate(pwd, encodePwd)) {
				resp.setStatus(2);
				logger.info("PBKDF2.validate Password error. email:{}|pwd:{}|status:{}",email,pwd,resp.getStatus());
				return resp;
			}
			
			
			String cookie = CookieUtil.encode(userId, CookieUtil.EXPIRE_SEC);
//			String proxyKey = CookieUtil.generateKey(userId, String.valueOf(System.currentTimeMillis()/1000), CookieUtil.EXPIRE_SEC);
//			String proxyAddr = CometScanner.take();
//			logger.info("proxykey:{} | size:{} | proxyAddr:{} ", proxyKey, proxyKey.getBytes().length, proxyAddr);
			resp.setStatus(0);
			resp.setUserId(userId);
//			result.setHeartBeat(CookieUtil.EXPIRE_SEC);
			resp.setCookie(cookie);
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

}
