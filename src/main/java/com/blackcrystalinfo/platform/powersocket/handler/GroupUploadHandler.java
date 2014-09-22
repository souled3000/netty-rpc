package com.blackcrystalinfo.platform.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.group.GroupUploadResponse;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class GroupUploadHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(GroupUploadHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		GroupUploadResponse resp = new GroupUploadResponse();
		resp.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String grpOld = HttpUtil.getPostValue(req.getParams(), "grpOld");
		String grpNew = HttpUtil.getPostValue(req.getParams(), "grpNew");
		String grpValue = HttpUtil.getPostValue(req.getParams(), "grpValue");
		String table = "user:group:" + userId;
		logger.info("GroupUploadHandler begin userId:{}|grpOld:{}|grpNew:{}|grpValue:{}",userId,grpOld,grpNew,grpValue);
		if (StringUtils.isBlank(userId) || (StringUtils.isBlank(grpOld) && StringUtils.isBlank(grpNew))) {
			resp.setStatus(0);
			logger.info("something is null. userId:{}|grpOld:{}|grpNew:{}|grpValue:{}|status:{}",userId,grpOld,grpNew,grpValue,resp.getStatus());
		} else {
			Jedis jedis = null;
			try {
				jedis = DataHelper.getJedis();

				Transaction tx = null;
				boolean b = jedis.hexists(table, grpOld);
				tx = jedis.multi();
				if (b) {
					tx.hdel(table, grpOld);
					resp.setStatus(3);
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						resp.setStatus(2);
					}
				} else {
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						resp.setStatus(1);
					}
				}
				tx.exec();
			} catch (Exception e) {
				DataHelper.returnBrokenJedis(jedis);
				resp.setStatus(-1);
				logger.error("Upload group info error.", e);
				return resp;
			} finally {
				DataHelper.returnJedis(jedis);
			}
		}
		logger.info("response: userId:{}|grpOld:{}|grpNew:{}|grpValue:{}|status:{}",userId,grpOld,grpNew,grpValue,resp.getStatus());
		return resp;
	}

}
