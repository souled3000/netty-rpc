package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

import redis.clients.jedis.Jedis;

/**
 * 情景模式
 * 
 * @author shenjizhe
 */
@Controller("/mobile/scene")
public class SceneApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(SceneApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();

		String userId = req.getUserId();
		String scenename = req.getParameter("sceneName");
		String scenecode = req.getParameter("sceneCode");
		String mac = req.getParameter("mac");

		Jedis j = null;
		try {
			j = JedisHelper.getJedis();

			// 添加 or 修改
			if (StringUtils.isNotBlank(scenename) && StringUtils.isNotBlank(scenecode) && StringUtils.isNotBlank(mac))
				j.hset("scene:" + userId, scenecode, scenename + ":" + mac);

			// 删除
			if (StringUtils.isBlank(scenename) && StringUtils.isNotBlank(scenecode) && StringUtils.isBlank(mac))
				j.hdel("scene:" + userId, scenecode);

			if (StringUtils.isBlank(scenename) && StringUtils.isBlank(scenecode) && StringUtils.isBlank(mac)) {
				String f = j.hget("user:family", userId);
				Map<String, String> sm = new HashMap<String, String>();
				if (StringUtils.isBlank(f)) {
					sm = j.hgetAll("scene:" + userId);
				} else {
					Set<String> mems = j.smembers("family:" + f);
					for (String m : mems) {
						Map<String, String> t = j.hgetAll("scene:" + m);
						sm.putAll(t);
					}
				}
				r.put("scene", sm);
			}

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.info("", e);
		} finally {
			JedisHelper.returnJedis(j);
		}

		return r;
	}
}
