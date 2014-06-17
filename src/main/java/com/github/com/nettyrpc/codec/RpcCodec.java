package com.github.com.nettyrpc.codec;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.com.nettyrpc.HandlerManager;
import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;

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
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;

public class RpcCodec extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(RpcCodec.class);

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (msg instanceof FullHttpRequest) {
			handleHttp(ctx, (FullHttpRequest) msg);
		} else {
			logger.debug("Bad request");
			ctx.close();
		}
	}

	private void handleHttp(ChannelHandlerContext ctx, FullHttpRequest req)
			throws Exception {
		if (!req.getDecoderResult().isSuccess()
				|| req.getUri().equals("/bad-request")
				|| req.getMethod() != HttpMethod.POST) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}
		if (is100ContinueExpected(req)) {
			send100Continue(ctx);
		}

		String reqUrl = req.getUri();
		HttpHeaders headers = req.headers();
		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
		IHandler handler = HandlerManager.getHandler(reqUrl);
		if (handler == null) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					NOT_FOUND));
			return;
		} else {
			Object ret = null;
			try {
				 ret = handler.rpc(new RpcRequest(reqUrl, decoder, headers,
						ctx.channel().remoteAddress().toString()));
			} catch (InternalException e) {
				logger.error("Called {} InternalError {}", reqUrl, e);
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					SERVICE_UNAVAILABLE, Unpooled.EMPTY_BUFFER));
			} finally {
				if (ret != null) {
					byte[] data = JSON.toJSONBytes(ret, new SerializerFeature[0]);
					sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
						OK, Unpooled.wrappedBuffer(data)));
				}
			}
		}
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx,
			FullHttpRequest req, FullHttpResponse res) {
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
					CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			setContentLength(res, res.content().readableBytes());
		}
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			 f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				CONTINUE);
		ctx.write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
