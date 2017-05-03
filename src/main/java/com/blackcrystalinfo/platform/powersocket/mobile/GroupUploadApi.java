package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0009;
import static com.blackcrystalinfo.platform.common.ErrorCode.C000A;
import static com.blackcrystalinfo.platform.common.ErrorCode.C000B;
import static com.blackcrystalinfo.platform.common.ErrorCode.C000C;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

@Controller("/mobile/gu")
public class GroupUploadApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(GroupUploadApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String userId = req.getUserId();
		String grpOld = req.getParameter("grpOld");
		String grpNew = req.getParameter("grpNew");
		String grpValue = req.getParameter("grpValue");
		String table = "user:group:" + userId;
		logger.info("GroupUploadHandler begin userId:{}|grpOld:{}|grpNew:{}|grpValue:{}", userId, grpOld, grpNew, grpValue);
		if (StringUtils.isBlank(userId) || (StringUtils.isBlank(grpOld) && StringUtils.isBlank(grpNew))) {
			r.put(status, C0009.toString());
			logger.info("grpOld与grpNew不能都为空");
		} else {
			Jedis j = null;
			try {
				j = JedisHelper.getJedis();
				Transaction tx = null;
				boolean b = j.hexists(table, grpOld);
				tx = j.multi();
				if (b) {
					tx.hdel(table, grpOld);
					r.put(status, C000C.toString());
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						r.put(status, C000B.toString());
					}
				} else {
					if (StringUtils.isNotBlank(grpNew)) {
						tx.hset(table, grpNew, grpValue);
						r.put(status, C000A.toString());
					}
				}
				tx.exec();
			} catch (Exception e) {
				logger.error("Upload group info error.", e);
				return r;
			} finally {
				JedisHelper.returnJedis(j);
			}
		}
		logger.info("response: userId:{}|grpOld:{}|grpNew:{}|grpValue:{}|status:{}", userId, grpOld, grpNew, grpValue, r.get(status));
		return r;
	}

}
