package com.blackcrystalinfo.platform.powersocket.api;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.DataHelper;
/**
 * 用户注册邮件确认
 * @author juliana
 *
 */
@Path(path="/cfm")
public class CfmApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(CfmApi.class);
	@Override
	public Object rpc(RpcRequest req) throws Exception {

		QueryStringDecoder decoder = new QueryStringDecoder(req.getUrl());  
        Map<String, List<String>> parame = decoder.parameters();
        List<String> q = parame.get("v"); // 读取从客户端传过来的参数  
		String sequences= q.get(0);
		Jedis j = null;
		try {
			j= DataHelper.getJedis();
			
			String email = j.get(sequences+"email");
			if(StringUtils.isBlank(email)){
				return fail();
			}
			
			String userId = String.valueOf(j.incrBy("user:nextid", 16));
			long intUserId = Long.parseLong(userId);
			if (intUserId % 16 > 0) {
				logger.info("userId can not modulo 16", userId);
				userId = String.valueOf(intUserId - intUserId % 16);
			}
			
			j.hset("user:mailtoid", email, sequences);
			j.hset("user:email", userId, email);
			j.del(sequences+"email");
			
			String phone = j.get(sequences+"phone");
			if (StringUtils.isNotBlank(phone)){
				j.hset("user:phone", userId, phone);
				j.del(sequences+"phone");
			}
			
			String nick = j.get(sequences+"nick");
			if (StringUtils.isNotBlank(nick)){
				j.hset("user:nick", userId, nick);
				j.del(sequences+"nick");
			}
			
			String shadow = j.get(sequences+"shadow");
			if (StringUtils.isNotBlank(shadow)){
				j.hset("user:shadow", userId, shadow);
				j.del(sequences+"shadow");
			}
			
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			return fail();
		} finally {
			DataHelper.returnJedis(j);
		}
		return succ();
	}
	
	private Object succ(){
		StringBuilder c = new StringBuilder();
		c.append("<!DOCTYPE html>");
		c.append("<html lang=\"zh-CN\">");
		c.append("<head>");
		c.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		c.append("</head>");
		c.append("<body>");
		c.append("<h1>恭喜成功注册</h1>");
		c.append("</body>");
		c.append("</html>");
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(c.toString().getBytes()));
		res.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-store");
		res.headers().set(HttpHeaders.Names.PRAGMA, "no-cache");
		res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		setContentLength(res, res.content().readableBytes());
		return res;
	}
	private Object fail(){
		StringBuilder c = new StringBuilder();
		c.append("<!DOCTYPE html>");
		c.append("<html lang=\"zh-CN\">");
		c.append("<head>");
		c.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		c.append("</head>");
		c.append("<body>");
		c.append("<h1>注册失败,可能因为激活已过期，请在30分钟内完成激活</h1>");
		c.append("</body>");
		c.append("</html>");
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(c.toString().getBytes()));
		res.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-store");
		res.headers().set(HttpHeaders.Names.PRAGMA, "no-cache");
		res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		setContentLength(res, res.content().readableBytes());
		return res;
	}
}
