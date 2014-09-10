package com.blackcrystalinfo.platform.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.group.GroupUploadResponse;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class GroupUploadHandler implements IHandler {

	private static final Logger logger = LoggerFactory.getLogger(GroupUploadHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		GroupUploadResponse result = new GroupUploadResponse();
		result.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String grpOld = HttpUtil.getPostValue(req.getParams(), "grpOld");
		String grpNew = HttpUtil.getPostValue(req.getParams(), "grpNew");
		String grpValue = HttpUtil.getPostValue(req.getParams(), "grpValue");
		String table = "user:group:" + userId;
		logger.info("GroupUploadHandler begin userId:{}|grpOld:{}|grpNew:{}|grpValue:{}",userId,grpOld,grpNew,grpValue);
		if (StringUtils.isBlank(userId) || (StringUtils.isBlank(grpOld) && StringUtils.isBlank(grpNew))) {
			result.setStatus(0);
		} else {
			Jedis jedis = null;
			try {
				jedis = DataHelper.getJedis();

				Transaction tx = null;
				boolean b = jedis.hexists(table, grpOld);
				tx = jedis.multi();
				if (b) {
					tx.hdel(table, grpOld);
					result.setStatus(3);
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						result.setStatus(2);
					}
				} else {
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						result.setStatus(1);
					}
				}
				tx.exec();
			} catch (Exception e) {
				DataHelper.returnBrokenJedis(jedis);
				result.setStatus(-1);
				logger.error("Upload group info error.", e);
				return result;
			} finally {
				DataHelper.returnJedis(jedis);
			}
		}
		logger.info("response: {}", result.getStatus());
		return result;
	}

}
