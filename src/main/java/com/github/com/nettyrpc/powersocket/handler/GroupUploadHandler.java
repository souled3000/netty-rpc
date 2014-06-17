package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.group.GroupUploadResponse;
import com.github.com.nettyrpc.util.HttpUtil;

public class GroupUploadHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(GroupUploadHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		GroupUploadResponse result = new GroupUploadResponse();

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String grpOld = HttpUtil.getPostValue(req.getParams(), "grpOld");
		String grpNew = HttpUtil.getPostValue(req.getParams(), "grpNew");
		String grpValue = HttpUtil.getPostValue(req.getParams(), "grpValue");
		String key = "user:group:" + userId;

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			if (jedis.hexists(key, grpOld)) {
				jedis.hdel(key, grpOld);
			}

			jedis.hset(key, grpNew, grpValue);

			result.setStatus(0);
			result.setUrlOrigin(req.getUrlOrigin());
		} catch (Exception e) {
			String msg = String.format("upload error, msg=", e.getMessage());
			result.setStatus(-1);
			result.setStatusMsg(msg);
			logger.error(msg);
		} finally {
			if (null != jedis) {
				jedis.disconnect();
			}
		}

		logger.info("response: {}", result);
		return result;
	}

}
