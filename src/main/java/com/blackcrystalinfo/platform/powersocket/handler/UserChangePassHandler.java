package com.blackcrystalinfo.platform.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserChangePassResponse;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.PBKDF2;

public class UserChangePassHandler implements IHandler {
	private static final Logger logger = LoggerFactory
			.getLogger(UserChangePassHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		UserChangePassResponse resp = new UserChangePassResponse();
		resp.setStatus(-1);
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String passOld = HttpUtil.getPostValue(req.getParams(), "passOld");
		String passNew = HttpUtil.getPostValue(req.getParams(), "passNew");
		logger.info("UserChangePassHandler begin userId:{}|passOld:{}|passNew:{}",userId,passOld,passNew);
		
		if(StringUtils.isBlank(userId)){
			resp.setStatus(3);
			logger.info("userId is null. userId:{}|passOld:{}|passNew:{}|status:{}",userId,passOld,passNew,resp.getStatus());
			return resp;
		}
		if(StringUtils.isBlank(passOld)){
			resp.setStatus(4);
			logger.info("passOld is null. userId:{}|passOld:{}|passNew:{}|status:{}",userId,passOld,passNew,resp.getStatus());
			return resp;
		}
		if(StringUtils.isBlank(passNew)){
			resp.setStatus(5);
			logger.info("passNew is null. userId:{}|passOld:{}|passNew:{}|status:{}",userId,passOld,passNew,resp.getStatus());
			return resp;
		}
		
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 用户密码
			String shadow = jedis.hget("user:shadow", userId);
			if (null == shadow) {
				resp.setStatus(1);
				logger.info("userId not exist. userId:{}|passOld:{}|passNew:{}|status:{}",userId,passOld,passNew,resp.getStatus());
				return resp;
			}

			// 2. 校验密码是否正确
			if (!PBKDF2.validate(passOld, shadow)) {
				resp.setStatus(2);
				logger.info("Password is incorrect. userId:{}|passOld:{}|passNew:{}|status:{}",userId,passOld,passNew,resp.getStatus());
				return resp;
			}

			// 3. 生成新密码
			String newShadow = PBKDF2.encode(passNew);
			jedis.hset("user:shadow", userId, newShadow);

			resp.setStatus(0);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("User change password error",e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: userId:{}|passOld:{}|passNew:{}|status:{}",userId,passOld,passNew,resp.getStatus());
		return resp;

	}

}
