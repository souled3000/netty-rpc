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
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.upgrade.UpgradeData;
import com.github.com.nettyrpc.powersocket.dao.pojo.upgrade.UpgradeInfoResponse;

public class UpgradeInfoHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UpgradeInfoHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("request: {}", req);

		UpgradeInfoResponse result = new UpgradeInfoResponse();

		String softId;
		String softInfo;
		String key = "software:upgrade";

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			Map<String, String> allGrpInfo = jedis.hgetAll(key);

			List<UpgradeData> uds = new ArrayList<UpgradeData>();
			Set<Entry<String, String>> entrySet = allGrpInfo.entrySet();
			for (Entry<String, String> entry : entrySet) {
				softId = entry.getKey();
				softInfo = entry.getValue();

				UpgradeData ud = new UpgradeData();
				ud.setSoftId(softId);
				ud.setSoftInfo(softInfo);

				uds.add(ud);
			}

			result.setStatus(0);
			result.setUpgradeDatas(uds);
			result.setUrlOrigin(req.getUrlOrigin());
		} catch (Exception e) {
			String msg = String.format("Get upgrade info failed, msg = %s",
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
