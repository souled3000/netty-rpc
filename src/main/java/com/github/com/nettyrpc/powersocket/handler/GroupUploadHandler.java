package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

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

			Transaction tx = null;
			if (jedis.hexists(key, grpOld)) {
				tx = jedis.multi();
				tx.hdel(key, grpOld);
				tx.hset(key, grpNew, grpValue);
				tx.exec();
			} else {
				jedis.hset(key, grpNew, grpValue);
			}

			result.setStatus(0);
			result.setUrlOrigin(req.getUrlOrigin());
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Upload group info error.");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}

}
