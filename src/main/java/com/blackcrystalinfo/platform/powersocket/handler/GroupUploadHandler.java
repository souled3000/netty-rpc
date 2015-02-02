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
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class GroupUploadHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(GroupUploadHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		GroupUploadResponse r = new GroupUploadResponse();
		r.setUrlOrigin(req.getUrlOrigin());

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String grpOld = HttpUtil.getPostValue(req.getParams(), "grpOld");
		String grpNew = HttpUtil.getPostValue(req.getParams(), "grpNew");
		String grpValue = HttpUtil.getPostValue(req.getParams(), "grpValue");
		String table = "user:group:" + userId;
		logger.info("GroupUploadHandler begin userId:{}|grpOld:{}|grpNew:{}|grpValue:{}", userId, grpOld, grpNew, grpValue);
		if (StringUtils.isBlank(userId) || (StringUtils.isBlank(grpOld) && StringUtils.isBlank(grpNew))) {
			r.setStatus(0);
			logger.info("something is null. userId:{}|grpOld:{}|grpNew:{}|grpValue:{}|status:{}", userId, grpOld, grpNew, grpValue, r.getStatus());
		} else {
			Jedis j = null;
			try {
				j = DataHelper.getJedis();

				String email = j.hget("user:email", userId);
				if (null == email) {
					r.setStatus(1);
					return r;
				}

				try {
					String shadow = j.hget("user:shadow", userId);
					if (!CookieUtil.validateMobileCookie(cookie, shadow, userId)) {
						r.setStatus(3);
						logger.info("user:shadow don't match user's ID. fId:{}|cookie:{}|status:{}", userId, cookie, r.getStatus());
						return r;
					}
				} catch (Exception e) {
					logger.error("user:shadow don't match user's ID. fId:{}|cookie:{}|status:{}", userId, cookie, r.getStatus(), e);
					return r;
				}

				Transaction tx = null;
				boolean b = j.hexists(table, grpOld);
				tx = j.multi();
				if (b) {
					tx.hdel(table, grpOld);
					r.setStatus(3);
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						r.setStatus(2);
					}
				} else {
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						r.setStatus(1);
					}
				}
				tx.exec();
			} catch (Exception e) {
				DataHelper.returnBrokenJedis(j);
				r.setStatus(-1);
				logger.error("Upload group info error.", e);
				return r;
			} finally {
				DataHelper.returnJedis(j);
			}
		}
		logger.info("response: userId:{}|grpOld:{}|grpNew:{}|grpValue:{}|status:{}", userId, grpOld, grpNew, grpValue, r.getStatus());
		return r;
	}

}
