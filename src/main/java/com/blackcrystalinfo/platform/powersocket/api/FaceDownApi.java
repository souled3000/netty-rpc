package com.blackcrystalinfo.platform.powersocket.api;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;

@Controller("/mobile/facedown")
public class FaceDownApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(FaceDownApi.class);

	public Object rpc(RpcRequest req) throws Exception {
		FullHttpResponse res = null;
		Long l = System.currentTimeMillis();
		String id = req.getParameter("uId");

		String facestamp = req.getParameter("facestamp");
		String facestampNew = null;

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			facestampNew = jedis.hget("user:facestamp", id);
		} catch (Exception e) {
			logger.error("get face stamp error !!!", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		// jedis 获取用户头像对应的标签
		if (StringUtils.equals(facestamp, facestampNew)) {
			res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
			return res;
		}

		// 下载头像
		File f = new File(Constants.PIC_PATH + File.separator + id);
		if (!f.exists()) {
			res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
			return res;
		}
		byte[] p = FileUtils.readFileToByteArray(f);
		System.out.println(System.currentTimeMillis() - l);
		res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(p));
		res.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-store");
		res.headers().set(HttpHeaders.Names.PRAGMA, "no-cache");
		res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
		res.headers().set("urlOrigin", "/mobile/facedown");
		setContentLength(res, res.content().readableBytes());
		return res;
	}
}
