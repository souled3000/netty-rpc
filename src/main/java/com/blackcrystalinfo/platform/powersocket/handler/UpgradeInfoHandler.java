package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.upgrade.UpgradeData;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.upgrade.UpgradeInfoResponse;

public class UpgradeInfoHandler implements IHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(UpgradeInfoHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("UpgradeInfoHandler: ");

		UpgradeInfoResponse result = new UpgradeInfoResponse();
		result.setStatus(-1);
		result.setUrlOrigin(req.getUrlOrigin());
		
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
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Get upgrade info error.");
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result.getStatus());
		return result;
	}

}
