package com.github.com.nettyrpc.powersocket.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.group.GroupData;
import com.github.com.nettyrpc.powersocket.dao.pojo.group.GroupInfoResponse;
import com.github.com.nettyrpc.util.HttpUtil;

public class GroupInfoHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(GroupInfoHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		logger.info("request: {}", req);

		GroupInfoResponse result = new GroupInfoResponse();

		String userId = HttpUtil.getPostValue(req.getParams(), "userId");

		String grpName;
		String grpValue;
		String key = "user:group:" + userId;

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			List<GroupData> gds = new ArrayList<GroupData>();
			Map<String, String> allGrpInfo = jedis.hgetAll(key);

			Set<Entry<String, String>> entrySet = allGrpInfo.entrySet();
			for (Entry<String, String> entry : entrySet) {
				grpName = entry.getKey();
				grpValue = entry.getValue();

				GroupData gd = new GroupData();
				gd.setGrpName(grpName);
				gd.setGrpValue(grpValue);

				gds.add(gd);
			}

			result.setStatus(0);
			result.setGroupDatas(gds);
			result.setUrlOrigin(req.getUrlOrigin());
		} catch (Exception e) {
			String msg = String.format("Get group info failed, msg = %s",
					e.getMessage());
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
