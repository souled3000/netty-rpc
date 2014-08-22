package com.blackcrystalinfo.platform.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.group.GroupUploadResponse;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class UpgradeUploadHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UpgradeUploadHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
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
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Upload upgrade info error.");
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result);
		return result;
	}

}
