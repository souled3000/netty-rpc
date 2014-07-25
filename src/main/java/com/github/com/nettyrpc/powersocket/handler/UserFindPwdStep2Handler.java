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
import com.github.com.nettyrpc.util.HttpUtil;
import com.github.com.nettyrpc.util.PBKDF2;

public class UserFindPwdStep2Handler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdStep2Handler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);
		UserFindPwdStep2Response resp = new UserFindPwdStep2Response();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());
		String userEmail = HttpUtil.getPostValue(req.getParams(), "email");
		String code = HttpUtil.getPostValue(req.getParams(), "code");
		String keyCode = new String(userEmail + "-code");
		String pwd = HttpUtil.getPostValue(req.getParams(), "pwd");
		if(StringUtils.isBlank(pwd)){
			resp.setStatus(4);
			return resp;
		}
		logger.info("new pwd: {}",pwd);
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			// 查找验证码
			if (!jedis.exists(keyCode)) {
				resp.setStatus(1);
				return resp;
			}
			
			//超三次验证失败直接返回
			String codeVal = jedis.get(keyCode);
			String strFailTime = jedis.get(keyCode+"fail");
			int failTime = Integer.valueOf(strFailTime==null?"0":strFailTime);
			if(failTime>=3){
				resp.setStatus(2);
				return resp;
			}
			//验证
			if(!codeVal.equals(code)){
				jedis.incr(keyCode+"fail");//累记失败次数
				resp.setStatus(3);
				return resp;
			}
			String userId = jedis.hget("user:mailtoid", userEmail);
			
			//生成新密码
			String newShadow = PBKDF2.encode(pwd);
			jedis.hset("user:shadow", userId, newShadow);

			resp.setStatus(0);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("", e);
			resp.setStatus(-1);
			return resp;
		} finally {
			DataHelper.returnJedis(jedis);
		}
		resp.setStatus(0);
		return resp;
	}

	private class UserFindPwdStep2Response extends ApiResponse {
	}
	
	public static void main(String[] args) throws Exception{
		System.out.println(PBKDF2.encode("123456"));
		System.out.println(PBKDF2.validate("123456","1000:5b42403636346365383938:179b4313a73cdf760137c112b05987e51387f4a7e8e3c8afaa3d5707bbb7187484038c332838ead052797eb45d6b18ecb13b2eb2d57882cf8fbe050765d28472"));
	}
}
