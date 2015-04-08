package com.blackcrystalinfo.platform.powersocket.api;

import io.netty.handler.codec.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.HttpUtil;
@Path(path="/sepia")
public class SepiaApi extends HandlerAdapter {
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		String cookie = req.getHeaders().get(HttpHeaders.Names.COOKIE);
		String sepia = HttpUtil.getPostValue(req.getParams(), "sepia");
		String code = HttpUtil.getPostValue(req.getParams(), "code");
		
		Jedis j = DataHelper.getJedis();
		Map ret = new HashMap();
		try{
			String word = j.get(cookie);
			Boolean isResponseCorrect = Boolean.FALSE;
//			isResponseCorrect = CaptchaServiceSingleton.getInstance().validateResponseForID(cookie, sepia.toUpperCase());
			isResponseCorrect = sepia.toUpperCase().equals(word);
			if (isResponseCorrect){
				j.setex(code+cookie, Constants.CAPTCHA_EXPIRE, "succ");
				ret.put("status", "0000");
			}
			return ret;
		}catch(Exception e){
			DataHelper.returnBrokenJedis(j);
		}finally{
			DataHelper.returnJedis(j);
		}
		ret.put("status", "ffff");
		return ret;
	}
}
