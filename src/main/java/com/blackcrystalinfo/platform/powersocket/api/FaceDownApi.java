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

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;

@Path(path="/mobile/facedown")
public class FaceDownApi extends HandlerAdapter {
	public Object rpc(RpcRequest req) throws Exception {
		Long l = System.currentTimeMillis();
		String id = req.getParameter( "uId");
		File f = new File(Constants.PIC_PATH + File.separator + id);
		FullHttpResponse res = null;
		if(!f.exists()){
			res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
			return res;
		}
		byte[] p = FileUtils.readFileToByteArray(f);
		System.out.println(System.currentTimeMillis() - l);
		res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(p));
		res.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-store");
		res.headers().set(HttpHeaders.Names.PRAGMA, "no-cache");
		res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
		res.headers().set("urlOrigin","/mobile/facedown");
		setContentLength(res, res.content().readableBytes());
		return res;
	}
}
