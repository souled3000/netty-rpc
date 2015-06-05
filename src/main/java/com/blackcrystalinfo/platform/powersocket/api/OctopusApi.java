package com.blackcrystalinfo.platform.powersocket.api;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
@Path(path="/octopus.jpg")
public class OctopusApi extends HandlerAdapter {
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		byte[] captchaChallengeAsJpeg = null;
		ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
		String cookie = UUID.randomUUID().toString();
		String word = Captcha.getWord();
		
		
//		BufferedImage challenge = CaptchaServiceSingleton.getInstance().getImageChallengeForID(cookie);
		BufferedImage challenge = Captcha.getImage(word);
		JPEGImageEncoder jpegEncoder = JPEGCodec.createJPEGEncoder(jpegOutputStream);
		jpegEncoder.encode(challenge);

		captchaChallengeAsJpeg = jpegOutputStream.toByteArray();
		
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(captchaChallengeAsJpeg));
		res.headers().set(HttpHeaders.Names.CACHE_CONTROL, "no-store");
		res.headers().set(HttpHeaders.Names.PRAGMA, "no-cache");
		res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
		
		res.headers().set(HttpHeaders.Names.SET_COOKIE,cookie);
		res.headers().set("urlOrigin","/octopus.jpg");
		setContentLength(res, res.content().readableBytes());
		
		Jedis j = DataHelper.getJedis();
		try{
			j.setex(cookie, Captcha.expire, word);
		}catch(Exception e){
			//DataHelper.returnBrokenJedis(j);
		}finally{
			DataHelper.returnJedis(j);
		}
		
		return res;
	}
}
