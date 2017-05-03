package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSONArray;
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

import redis.clients.jedis.Jedis;

@Controller("/mobile/gd")
public class GroupDownloadApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(GroupDownloadApi.class);

	@SuppressWarnings("rawtypes")
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String userId = req.getUserId();

		String grpName;
		String grpValue;
		String key = "user:group:" + userId;

		Jedis j = null;
		try {
			j = JedisHelper.getJedis();

			List<Map> gds = new ArrayList<Map>();
			Map<String, String> allGrpInfo = j.hgetAll(key);

			Set<Entry<String, String>> entrySet = allGrpInfo.entrySet();
			for (Entry<String, String> entry : entrySet) {
				grpName = entry.getKey();
				grpValue = entry.getValue();
				List<Map> ds = JSONArray.parseArray(grpValue, Map.class);
				Map<String, Object> gd = new HashMap<String, Object>();
				gd.put("grpName", grpName);
				gd.put("grpValue", ds);
				gds.add(gd);
			}

			r.put("groupDatas", gds);
		} catch (Exception e) {
			logger.error("", e);
			return r;
		} finally {
			JedisHelper.returnJedis(j);
		}
		r.put(status, SUCCESS.getCode());
		return r;
	}
}
