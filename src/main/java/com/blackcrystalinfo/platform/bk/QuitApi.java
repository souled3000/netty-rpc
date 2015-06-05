package com.blackcrystalinfo.platform.bk;

import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;

@Path(path="/mobile/quit")
public class QuitApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(QuitApi.class);

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String cookie = req.getParameter( "cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		String fid = req.getParameter( "fid");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			if(StringUtils.isBlank(fid)){
				r.put(status, ErrorCode.C0029);
				return r;
			}
			long x = j.zrem(userId, fid);
			long y = j.zrem(fid + "u", userId);
			
			if(x==0&&y>0){
				r.put(status, ErrorCode.C001A);
				return r;
			}
			if(y==0&&x>0){
				r.put(status, ErrorCode.C001B);
				return r;
			}
			if(y==0&&x==0){
				r.put(status, ErrorCode.C001C);
				return r;
			}
		} catch (Exception e) {
			//DataHelper.returnBrokenJedis(j);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, ErrorCode.SUCCESS);
		return r;
	}
}
