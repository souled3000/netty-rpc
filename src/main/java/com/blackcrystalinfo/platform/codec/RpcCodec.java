package com.blackcrystalinfo.platform.codec;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.RespField.status;
import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
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
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;
import com.blackcrystalinfo.platform.util.cryto.Datagram;

@Service("rpcCodec")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RpcCodec extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(RpcCodec.class);

	@Autowired
	private ApplicationContext ctx;

	private Object getBean(String beanName) {
		if ("".equals(beanName))
			return null;
		try {
			return ctx.getBean(beanName, IHandler.class);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("get bean exception.", e);
			}
		}
		return null;
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {

		try {
			if (msg instanceof FullHttpRequest) {
				handleHttp(ctx, (FullHttpRequest) msg);
			} else {
				logger.debug("Not http request.");
				ctx.close();
			}
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	public static final int HTTP_CACHE_SECONDS = 60;

	private void handleHttp(ChannelHandlerContext ctx, FullHttpRequest req)
			throws Exception {
		if (!req.getDecoderResult().isSuccess()
				|| req.getUri().equals("/bad-request")
				|| (req.getMethod() != HttpMethod.POST
						&& !req.getUri().equals("/octopus.jpg")
						&& !req.getUri().equals("/pic") && !req.getUri()
						.startsWith("/cfm"))) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}
		if (is100ContinueExpected(req)) {
			send100Continue(ctx);
		}
		RpcRequest rp = null;
		String reqUrl = req.getUri();
		logger.info("request(new)--{}--{}", reqUrl, req.getMethod());
		HttpHeaders headers = req.headers();

		HttpPostRequestDecoder decoder = null;
		if (req.getMethod() == HttpMethod.POST)
			decoder = new HttpPostRequestDecoder(req);
		IHandler validateHandler = (IHandler) this.getBean(reqUrl.substring(0,
				reqUrl.lastIndexOf("/")));
		IHandler handler = null;

		if (req.getUri().contains("?"))
			handler = (IHandler) this.getBean(req.getUri().substring(0,
					req.getUri().indexOf("?")));
		else
			handler = (IHandler) this.getBean(req.getUri());

		if (handler == null) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					NOT_FOUND));
			logger.info("(404)--{}--{}", reqUrl, req.getMethod());
			return;
		} else {
			Object ret = null;
			Datagram reqDatagram = null;
			try {
				rp = new RpcRequest(reqUrl, decoder, headers, ctx.channel()
						.remoteAddress().toString());

				logger.debug("{} - {}", reqUrl, rp.toString());
				reqDatagram = rp.getDatagram();
				if (reqDatagram == null) {
					if (validateHandler != null) {
						Map<?, ?> m = (Map<?, ?>) validateHandler.rpc(rp);
						String r = (String) m.get(status);
						if (r.equals(SUCCESS.toString()))
							ret = handler.rpc(rp);
						else
							ret = m;
					} else {
						ret = handler.rpc(rp);
					}
				} else {
					ret = handler.rpc(JSONObject.parseObject(reqDatagram
							.getCtn()));
					reqDatagram.setCtn(ByteUtil.writeJSON(ret));
					try {
						reqDatagram.encapsulate();
					} catch (Exception e) {
						e.printStackTrace();
					}
					ret = reqDatagram;
				}
			} catch (Exception e) {
				logger.error("(400) {} ", reqUrl, e);
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
						HTTP_1_1, BAD_REQUEST));
			} finally {
				if (ret != null) {
					logger.info("url:{} | request:{} | response:{}", reqUrl,
							rp.toString(), JSON.toJSONString(ret));
					// 不需要返回请求的url，去掉了
					// if (ret instanceof Map) {
					// Map m = (Map) ret;
					// m.put("urlOrigin", reqUrl);
					// }
					if (ret instanceof FullHttpResponse) {
						sendHttpResponse2(ctx, req, (FullHttpResponse) ret);
						return;
					}
					sendHttpResponse(
							ctx,
							req,
							new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled
									.wrappedBuffer(JSON.toJSONBytes(ret,
											new SerializerFeature[0]))));
				} else {
					logger.error("(503)ret is null: {} | param:{}", reqUrl,
							rp.toString());
					sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
							HTTP_1_1, SERVICE_UNAVAILABLE));
				}
			}
		}
	}

	private static void sendHttpResponse2(ChannelHandlerContext ctx,
			FullHttpRequest req, FullHttpResponse res) {
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
					CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx,
			FullHttpRequest req, FullHttpResponse res) {
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
					CharsetUtil.UTF_8);
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
