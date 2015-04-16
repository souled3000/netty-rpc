package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0009;
import static com.blackcrystalinfo.platform.util.ErrorCode.C000A;
import static com.blackcrystalinfo.platform.util.ErrorCode.C000B;
import static com.blackcrystalinfo.platform.util.ErrorCode.C000C;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Path(path="/mobile/gu")
public class GroupUploadApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(GroupUploadApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String cookie = req.getParameter( "cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		String grpOld = req.getParameter( "grpOld");
		String grpNew = req.getParameter( "grpNew");
		String grpValue = req.getParameter( "grpValue");
		String table = "user:group:" + userId;
		logger.info("GroupUploadHandler begin userId:{}|grpOld:{}|grpNew:{}|grpValue:{}", userId, grpOld, grpNew, grpValue);
		if (StringUtils.isBlank(userId) || (StringUtils.isBlank(grpOld) && StringUtils.isBlank(grpNew))) {
			r.put(status, C0009.toString());
			logger.info("grpOld与grpNew不能都为空");
		} else {
			Jedis j = null;
			try {
				j = DataHelper.getJedis();
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
				DataHelper.returnBrokenJedis(j);
				logger.error("Upload group info error.", e);
				return r;
			} finally {
				DataHelper.returnJedis(j);
			}
		}
		logger.info("response: userId:{}|grpOld:{}|grpNew:{}|grpValue:{}|status:{}", userId, grpOld, grpNew, grpValue, r.get(status));
		return r;
	}

}
