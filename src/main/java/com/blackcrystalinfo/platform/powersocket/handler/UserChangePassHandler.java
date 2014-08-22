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

		UserChangePassResponse result = new UserChangePassResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String passOld = HttpUtil.getPostValue(req.getParams(), "passOld");
		String passNew = HttpUtil.getPostValue(req.getParams(), "passNew");

		if(StringUtils.isBlank(userId)){
			result.setStatus(3);
			return result;
		}
		if(StringUtils.isBlank(passOld)){
			result.setStatus(4);
			return result;
		}
		if(StringUtils.isBlank(passNew)){
			result.setStatus(5);
			return result;
		}
		
		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 1. 用户密码
			String shadow = jedis.hget("user:shadow", userId);
			if (null == shadow) {
				result.setStatusMsg("userId not exist.");
				result.setStatus(1);
				return result;
			}

			// 2. 校验密码是否正确
			if (!PBKDF2.validate(passOld, shadow)) {
				result.setStatusMsg("Password is incorrect.");
				result.setStatus(2);
				return result;
			}

			// 3. 生成新密码
			String newShadow = PBKDF2.encode(passNew);
			jedis.hset("user:shadow", userId, newShadow);

			result.setStatus(0);

		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("User change password error");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;

	}

}
