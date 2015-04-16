package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Path(path="/mobile/gd")
public class GroupDownloadApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(GroupDownloadApi.class);

	@SuppressWarnings("rawtypes")
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());
		
		String cookie = req.getParameter( "cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);

		String grpName;
		String grpValue;
		String key = "user:group:" + userId;

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

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
			DataHelper.returnBrokenJedis(j);
			logger.error("",e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, SUCCESS.getCode());
		return r;
	}
	
	public static void main(String[] args) {
		Jedis j= DataHelper.getJedis();
		Map<Object,Object> r = new HashMap<Object,Object>();
		List<Map> gds = new ArrayList<Map>();
		Map<String, String> allGrpInfo = j.hgetAll("user:group:64");

		Set<Entry<String, String>> entrySet = allGrpInfo.entrySet();
		for (Entry<String, String> entry : entrySet) {
		String	grpName = entry.getKey();
		String	grpValue = entry.getValue();
			List<Map> ds = JSONArray.parseArray(grpValue, Map.class);
			Map<String, Object> gd = new HashMap<String, Object>();
			gd.put("grpName", grpName);
			gd.put("grpValue", ds);
			gds.add(gd);
		}

		r.put("groupDatas", gds);
		
		System.out.println(JSON.toJSONString(r));
		DataHelper.returnJedis(j);
	}
}
