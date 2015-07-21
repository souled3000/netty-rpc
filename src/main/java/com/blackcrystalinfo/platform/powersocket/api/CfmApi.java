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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.BizCode;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.DateUtils;
import com.blackcrystalinfo.platform.util.Utils;

/**
 * 用户注册邮件确认
 * @author juliana
 *
 */
@Controller("/cfm")
public class CfmApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(CfmApi.class);
	
	@Autowired
	ILoginSvr loginSvr;
	
	@Override
	public Object rpc(RpcRequest req) throws Exception {

		QueryStringDecoder decoder = new QueryStringDecoder(req.getUrl());  
        Map<String, List<String>> parame = decoder.parameters();
        List<String> q = parame.get("v"); // 读取从客户端传过来的参数  
		String sequences= q.get(0);
		Jedis j = null;
		try {
			j= DataHelper.getJedis();
			
			String uid = j.get("user:mailActive:"+sequences);
			if (StringUtils.isBlank(uid)) {
				return fail();
			}
			
			User user = loginSvr.userGet(User.UserIDColumn, uid);
			String email = user.getEmail();
			if(StringUtils.isBlank(email)){
				return fail();
			}
			loginSvr.userChangeProperty(uid, User.UserEmailableShadowColumn, "true");
			j.del("user:activetimes:" + uid);
			j.del("user:mailActiveUUID:" + uid); //激活了，用户-》UUID 这个记录要删除
			j.del("user:mailActive:" + sequences); //激活了，这个链接就没用了，下次调用直接fail
			j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg(uid+"|",BizCode.UserActivateSuccess.getValue(), Integer.parseInt(uid), ""));
		} catch (Exception e) {
			//DataHelper.returnBrokenJedis(j);
			e.printStackTrace();
			logger.error("emai active failed!!!", e);
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
		c.append("<h1>恭喜成功激活</h1>");
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
		c.append("<h1>激活失败,可能因为连接已失效；也可能因为激活已过期，请在");
		c.append(DateUtils.secToTime(Constants.MAIL_ACTIVE_EXPIRE));
		c.append("内完成激活</h1>");
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
