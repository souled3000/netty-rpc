package com.blackcrystalinfo.platform.codec;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.activation.MimetypesFileTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.blackcrystalinfo.platform.HandlerManager;
import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;
import com.blackcrystalinfo.platform.util.cryto.Datagram;

public class RpcCodec extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(RpcCodec.class);

	private final static String VERSIONFILE = Constants.getProperty("version.file", "");

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			handleHttp(ctx, (FullHttpRequest) msg);
		} else {
			logger.debug("Bad request");
			ctx.close();
		}
	}

	private static void setContentTypeHeader(HttpResponse response, File file) {
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
	}

	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	public static final int HTTP_CACHE_SECONDS = 60;

	private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		// Date header
		Calendar time = new GregorianCalendar();
		response.headers().set(DATE, dateFormatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
		response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
		response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
		response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
	}

	private void handleHttp(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		if (!req.getDecoderResult().isSuccess() || req.getUri().equals("/bad-request") || req.getMethod() != HttpMethod.POST) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}
		if (is100ContinueExpected(req)) {
			send100Continue(ctx);
		}

		String reqUrl = req.getUri();
		logger.info("request(new)--{}", reqUrl);
		HttpHeaders headers = req.headers();
		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
		IHandler handler = HandlerManager.getHandler(reqUrl);
		if (handler == null) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
			return;
		} else {
			Object ret = null;
			Datagram resp = null;
			try {
				RpcRequest rp = new RpcRequest(reqUrl, decoder, headers, ctx.channel().remoteAddress().toString());
				resp = HttpUtil.getDatagram(rp);
				if (resp == null) {
					ret = handler.rpc(rp);
				} else {
					logger.debug(handler.getClass().toString());
					ret = handler.rpc(JSONObject.parseObject(resp.getCtn()));
					resp.setCtn(ByteUtil.writeJSON(ret));
					try {
						resp.encapsulate();
					} catch (Exception e) {
						e.printStackTrace();
					}
					ret = resp;
				}
			} catch (Exception e) {
				logger.error("Called {} InternalError {}", reqUrl, e);
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			} finally {
				if (ret != null) {
					byte[] data = JSON.toJSONBytes(ret, new SerializerFeature[0]);
					sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(data)));
				} else {
					logger.error("Called {}", reqUrl);
					sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, SERVICE_UNAVAILABLE));
				}
			}
		}
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		setContentLength(res, res.content().readableBytes());
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
		ctx.write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
