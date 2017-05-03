package com.blackcrystalinfo.ws;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import com.blackcrystalinfo.platform.util.MiscUtils;

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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;

public class DevWs {
//	public final SocketAddress serve = new InetSocketAddress("123.57.47.53", 1771);
	public final SocketAddress serve = new InetSocketAddress("localhost", 8080);
//	public final SocketAddress serve = new InetSocketAddress("192.168.2.13", 1771);
	public final String wsurl = "/ws";
	public final SocketAddress ME = new InetSocketAddress("193.168.1.185", 20002);

	public static void main(String[] args) throws Exception {
		
//		byte[] a =MiscUtils.fromHex("474554202f777320485454502f312e310d0a557067726164653a20776562736f636b65740d0a436f636e656374696f6e3a20557067726164650d0a5365632d576562536f636b65742d4b65793a20736e7576342b436335754f46433432345a6b455443413d3d0d0a486f73743a206e756c6c3a38300d0a5365632d576562536f636b65742d4f726967696e3a206874703a2f2f6e756c6c0d0a5365632d576562536f636b65742d56657273696f6e3a2031330d0a0d0a");
		byte[] a =MiscUtils.fromHex("7167376e327149427c306137303361336335366131494f53");
//		byte[] a = new byte[]{50, 51, 55, 124, 13, -25, -1, -1, -1, -1, -1, -1, -1, 0};
		System.out.println(new String(a));
		System.out.println(MiscUtils.toHex(a));
		
		String s = "2pf86hv0|1f14d9675e47IOS";
		System.out.println(MiscUtils.toHex(s.getBytes()));
		System.out.println(new String(s.getBytes()));
		
		go();
	}

	public static void go() throws Exception {	

		DevWs client = new DevWs();
		final ChannelGroup group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
		client.boot(group);

//		Thread.currentThread().sleep(2000);

		System.out.println("send login request");
		ByteBuf bb = Unpooled.buffer(1000);
		// byte[] data =
		// MiscUtils.fromHex("c20006004e2d020002004e2d02004e2d02006ebc4a9876f904008c2daa00db8d57b51fb46b2c1c9e42cf20ef7cc5c86818b314f30aa016d154f18b9e0a6dfc37ee44106a7898d4d2016a7674a3a6ef8e14eb1295d55addd4cd3d0f94f390aa290cfdf78b83a9437920699e0fbd60ead4becfd010983f815f994605fa64ff6ba558e3219575957c4b3377aba12bbb53cd8c763e007a35cda1bb55a9aa2bb368c4e10966b4436e1337f31afc42e9618f71ef03fed0c13bfa7451a273927d2e7a177869ee061b8ee0987029f0d0345c0270");
		byte[] data = MiscUtils.fromHex("c2000700443202004432020044320200443222910caa3ad44232c1007c32bdbce7b3f353fd2f6f36a2a8d4fde810e984b7a27d2ad32ff44d2d5ca3ad8c55c85c7afbc4d5d30cc27ffaae551d7b33e35a5dce139438d1c29d354932736f20fdb8");
		bb.writeBytes(data);
		BinaryWebSocketFrame bwf =new BinaryWebSocketFrame(bb);
		group.writeAndFlush(bwf);

		
		for (int i =0;i<1000;i++){
			bb = Unpooled.buffer(1000);
			bb.writeBytes(data);
			group.writeAndFlush(new BinaryWebSocketFrame(bb));
			System.out.println(i);
//			Thread.sleep(1);
		}
		
//		Timer timer = new Timer();
//		timer.schedule(new TimerTask() {
//			public void run() {
//				ByteBuf bb = Unpooled.buffer(1000);
//				// byte[] data =
//				// MiscUtils.fromHex("c20006004e2d020002004e2d02004e2d02006ebc4a9876f904008c2daa00db8d57b51fb46b2c1c9e42cf20ef7cc5c86818b314f30aa016d154f18b9e0a6dfc37ee44106a7898d4d2016a7674a3a6ef8e14eb1295d55addd4cd3d0f94f390aa290cfdf78b83a9437920699e0fbd60ead4becfd010983f815f994605fa64ff6ba558e3219575957c4b3377aba12bbb53cd8c763e007a35cda1bb55a9aa2bb368c4e10966b4436e1337f31afc42e9618f71ef03fed0c13bfa7451a273927d2e7a177869ee061b8ee0987029f0d0345c0270");
//				byte[] data = MiscUtils.fromHex("c2000700443202004432020044320200443222910caa3ad44232c1007c32bdbce7b3f353fd2f6f36a2a8d4fde810e984b7a27d2ad32ff44d2d5ca3ad8c55c85c7afbc4d5d30cc27ffaae551d7b33e35a5dce139438d1c29d354932736f20fdb8");
//				bb.writeBytes(data);
//				group.writeAndFlush(new BinaryWebSocketFrame(bb));
////				group.writeAndFlush(new PingWebSocketFrame(b));
////				group.writeAndFlush(new PingWebSocketFrame());
//			}
//		}, 0, 1);
	}

	public void boot(ChannelGroup group) throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup()).channel(NioSocketChannel.class).handler(new ChatServerInitializer(group));
//		bootstrap.localAddress(ME);
		ChannelFuture future = bootstrap.connect(serve);
		System.out.println("server:" + serve.toString());
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
			pipeline.addLast(new WebSocketFrameHandler(group));
		}

		class WebSocketFrameHandler extends ChannelInboundHandlerAdapter {
			private final ChannelGroup group;

			public WebSocketFrameHandler(ChannelGroup group) {
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
//					System.out.println("RECV:"+ByteUtil.toHex(tt).toLowerCase());
					System.out.println("RECV:"+tt.length);
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
