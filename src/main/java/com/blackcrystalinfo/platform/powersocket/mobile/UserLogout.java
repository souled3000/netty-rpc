package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.LogType;
import com.blackcrystalinfo.platform.powersocket.log.ILogger;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

import redis.clients.jedis.Jedis;

@Controller("/mobile/logout")
public class UserLogout extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(UserLogout.class);
	@Autowired
	ILogger log;
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		Jedis j = null;
		String userId = req.getUserId();
		try{
//			j=DataHelper.getJedis();
//			j.del("user:cookie:" + userId);
		}catch(Exception e ){
			return r;
		}finally{
			DataHelper.returnJedis(j);
		}
		r.put(status, SUCCESS.toString());
//		logger.info("{}|{}|{}",userId,System.currentTimeMillis(),"登出成功");
		log.write(String.format("%s|%s|%s|%s", userId,System.currentTimeMillis(),LogType.ZCDL,"登出成功"));
		return r;
	}

}
