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
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class GroupInfoHandler extends HandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(GroupInfoHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		GroupInfoResponse r = new GroupInfoResponse();
		r.setStatus(-1);
		
		String userId = HttpUtil.getPostValue(req.getParams(), "userId");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		logger.info("GroupInfoHandler begin userId:{}",userId);
		
		if(StringUtils.isBlank(userId)){
			r.setStatus(1);
			logger.info("userId is null. userId:{}",userId);
			return r;
		}
		
		String grpName;
		String grpValue;
		String key = "user:group:" + userId;

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String email = j.hget("user:email", userId);
			if (null == email) {
				r.setStatus(1);
				logger.info("user:shadow don't match user's ID. fId:{}|cookie:{}|status:{}", userId, cookie, r.getStatus());
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
			
			List<GroupData> gds = new ArrayList<GroupData>();
			Map<String, String> allGrpInfo = j.hgetAll(key);

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

			r.setStatus(0);
			r.setGroupDatas(gds);
			
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("Get Group info error. userId:{}",userId,e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		logger.info("response: {}", r.getStatus());
		return r;
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
