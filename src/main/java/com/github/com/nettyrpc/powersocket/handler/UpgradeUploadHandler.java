package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.group.GroupUploadResponse;
import com.github.com.nettyrpc.util.HttpUtil;

public class UpgradeUploadHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UpgradeUploadHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		logger.info("request: {}", req);

		GroupUploadResponse result = new GroupUploadResponse();

		String softId = HttpUtil.getPostValue(req.getParams(), "softId");
		String softInfo = HttpUtil.getPostValue(req.getParams(), "softInfo");
		String key = "software:upgrade";

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			jedis.hset(key, softId, softInfo);

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
