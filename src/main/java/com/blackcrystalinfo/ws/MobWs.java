package com.blackcrystalinfo.ws;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class MobWs {
	public final SocketAddress serve = new InetSocketAddress("123.57.47.53", 1234);
	public final String wsurl = "ws://123.57.47.53:1234/ws";
	public final SocketAddress lclAdr = new InetSocketAddress("193.168.1.185", 20002);

	public static void main(String[] args) throws Exception {
		f2();
	}

	public static void f2() throws Exception {

		MobWs client = new MobWs();
		final ChannelGroup group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		client.boot(group);
		final ByteBuf bb = Unpooled.buffer(32);
		bb.writeBytes(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 });
		group.writeAndFlush(new BinaryWebSocketFrame(bb));// 向设备转发消息
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				group.writeAndFlush(new PingWebSocketFrame());

			}
		}, 0, 14000);
		System.out.println("finsish");

	}

	public static void f() throws Exception {
		URL url = new URL("http://192.168.2.16:8181/mobile/cometadr");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		DataOutputStream out = new DataOutputStream(con.getOutputStream());
		// The URL-encoded contend
		// 正文，正文内容其实跟get的URL中'?'后的参数字符串一致
		String content = "cookie=" + URLEncoder.encode("NXwzMDB8MTQ0ODAxNDkyMzQ0M3wwM2QzOGM4MWNhZTIzNzQ5Y2FjMWIzYjI4YWY4NTg1YWEyOGVmMQ==-C273B33C747E84EE6F4CB4F280A81FE5", "utf-8");
		// DataOutputStream.writeBytes将字符串中的16位的 unicode字符以8位的字符形式写道流里面
		out.writeBytes(content);
		con.connect();
		String s = con.getResponseMessage();
		System.out.println(s);
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		System.out.println(" ============================= ");
		System.out.println(" Contents of get request ");
		System.out.println(" ============================= ");
		String lines;
		out.flush();
		out.close(); // flush and close
		lines = reader.readLine();
		reader.close();
		// 断开连接
		con.disconnect();

		String proxyKey = JSON.parseObject(lines).getString("proxyKey");
		MobWs client = new MobWs();
		final ChannelGroup group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		client.boot(group);
		Thread.sleep(2000);
		final ByteBuf bb = Unpooled.buffer(32);
		bb.writeBytes(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 });
		group.writeAndFlush(new TextWebSocketFrame(proxyKey));// ws登录
		group.writeAndFlush(new BinaryWebSocketFrame(bb));// 向设备转发消息
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				group.writeAndFlush(new PingWebSocketFrame());

			}
		}, 0, 14000);
		System.out.println("finsish");

	}

	public void boot(ChannelGroup group) throws Exception {

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class).handler(new ChatServerInitializer(group));
		bootstrap.localAddress(lclAdr);
		ChannelFuture future = bootstrap.connect(serve);
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				if (channelFuture.isSuccess()) {
					System.out.println("Connection established");
				} else {
					System.err.println("Connection attempt failed");
					channelFuture.cause().printStackTrace();
				}
			}
		});
	}

	class ChatServerInitializer extends ChannelInitializer<Channel> {
		private final ChannelGroup group;

		public ChatServerInitializer(ChannelGroup group) {
			this.group = group;
		}

		@Override
		protected void initChannel(Channel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new HttpClientCodec());
			pipeline.addLast(new ChunkedWriteHandler());
			pipeline.addLast(new HttpObjectAggregator(64 * 1024));
			URI uri = new URI(wsurl);
			WebSocketClientHandshaker shaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, "", false, null);
			pipeline.addLast(new WebSocketClientProtocolHandler(shaker));
			// pipeline.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, "", false, null, 3000, false));
			pipeline.addLast(new TextWebSocketFrameHandler(group));
		}

		class TextWebSocketFrameHandler extends ChannelInboundHandlerAdapter {
			private final ChannelGroup group;

			public TextWebSocketFrameHandler(ChannelGroup group) {
				this.group = group;
			}

			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
					group.writeAndFlush(new TextWebSocketFrame("Client " + ctx.channel() + " joined"));
					group.add(ctx.channel());
					System.out.println(evt.toString());
					System.out.println("finish shaker");
				} else {
					System.out.println(evt.toString());
					super.userEventTriggered(ctx, evt);
				}
			}

			public void channelRead(ChannelHandlerContext ctx, Object m) throws Exception {
				BinaryWebSocketFrame msg = (BinaryWebSocketFrame) m;
				// System.out.println(msg.text());
				ByteBuf bb = msg.content();
				if (!bb.hasArray()) {
					byte[] tt = new byte[bb.readableBytes()];
					bb.getBytes(0, tt);
					System.out.println(ByteUtil.toHex(tt));
				} else {
					System.out.println("nothing can be readed");
				}
				ReferenceCountUtil.release(m);
				// group.writeAndFlush(msg.retain());
			}

			@Override
			public void channelActive(ChannelHandlerContext ctx) throws Exception {
				super.channelActive(ctx);
				System.out.println("active");
			}

			@Override
			public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
				super.channelReadComplete(ctx);
				// ctx.read();
				// System.out.println("ReadComplete");
			}

		}
	}
}
