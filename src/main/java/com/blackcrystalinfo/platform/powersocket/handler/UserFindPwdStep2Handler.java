package com.blackcrystalinfo.platform.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.PBKDF2;

public class UserFindPwdStep2Handler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdStep2Handler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		UserFindPwdStep2Response resp = new UserFindPwdStep2Response();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());
		String userEmail = HttpUtil.getPostValue(req.getParams(), "email");
		String code = HttpUtil.getPostValue(req.getParams(), "code");
		String keyCode = new String(userEmail + "-code");
		String pwd = HttpUtil.getPostValue(req.getParams(), "pwd");
		
		logger.info("UserFindPwdStep2Handler begin userEmail:{}|code:{}|keyCode:{}|pwd:{}", userEmail,code,keyCode,pwd);
		
		if(StringUtils.isBlank(pwd)){
			resp.setStatus(4);
			logger.info("pwd is null. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail,code,keyCode,pwd,resp.getStatus());
			return resp;
		}
		if(StringUtils.isBlank(code)){
			resp.setStatus(5);
			logger.info("code is null. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail,code,keyCode,pwd,resp.getStatus());
			return resp;
		}
		if(StringUtils.isBlank(userEmail)){
			resp.setStatus(6);
			logger.info("userEmail is null. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail,code,keyCode,pwd,resp.getStatus());
			return resp;
		}
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			// 验证码是否过期
			if (!jedis.exists(keyCode)) {
				resp.setStatus(1);
				logger.info("code has been expired. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail,code,keyCode,pwd,resp.getStatus());
				return resp;
			}
			
			//超三次验证失败直接返回
			String codeVal = jedis.get(keyCode);
			String strFailTime = jedis.get(keyCode+"fail");
			int failTime = Integer.valueOf(strFailTime==null?"0":strFailTime);
			if(failTime>=3){
				resp.setStatus(2);
				logger.info("the times validating beyond three. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail,code,keyCode,pwd,resp.getStatus());
				return resp;
			}
			//验证
			if(!codeVal.equals(code)){
				jedis.incr(keyCode+"fail");//累记失败次数
				resp.setStatus(3);
				logger.info("validating fail. userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail,code,keyCode,pwd,resp.getStatus());
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
		logger.info("response: userEmail:{}|code:{}|keyCode:{}|pwd:{}|status:{}", userEmail,code,keyCode,pwd,resp.getStatus());
		return resp;
	}

	private class UserFindPwdStep2Response extends ApiResponse {
	}
}
