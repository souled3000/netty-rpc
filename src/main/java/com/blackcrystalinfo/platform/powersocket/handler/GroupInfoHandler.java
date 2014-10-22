package com.blackcrystalinfo.platform.powersocket.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONArray;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.group.GroupData;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.group.GroupDevice;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.group.GroupInfoResponse;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class GroupInfoHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(GroupInfoHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		GroupInfoResponse result = new GroupInfoResponse();
		result.setStatus(-1);
		result.setUrlOrigin(req.getUrlOrigin());
		
		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		logger.info("GroupInfoHandler begin userId:{}",userId);
		
		if(StringUtils.isBlank(userId)){
			result.setStatus(1);
			logger.info("userId is null. userId:{}",userId);
			return result;
		}
		
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
				List<GroupDevice> ds = JSONArray.parseArray(grpValue, GroupDevice.class);
				GroupData gd = new GroupData();
				gd.setGrpName(grpName);
				gd.setGrpValue(ds);
				gds.add(gd);
			}

			result.setStatus(0);
			result.setGroupDatas(gds);
			
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("Get Group info error. userId:{}",userId,e);
			return result;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: {}", result.getStatus());
		return result;
	}
	public static void main(String[] args) throws Exception{
		Jedis jedis = DataHelper.getJedis();
		Map<String, String> allGrpInfo = jedis.hgetAll("user:group:1");
		for (Entry<String, String> entry : allGrpInfo.entrySet()) {
			String grpName = entry.getKey();
			String grpValue = entry.getValue();
			
			logger.info("grpName:{}|grpVal:{}",grpName,grpValue);
			
			
			List<GroupDevice> ds = JSONArray.parseArray(grpValue, GroupDevice.class);
			
			
			for(GroupDevice d : ds){
				logger.info("name:{}|mac:{}",d.getDeviceName(),d.getMac());
			}
		}
		DataHelper.returnJedis(jedis);
	}
}
