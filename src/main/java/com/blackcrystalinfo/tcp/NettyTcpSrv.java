package com.blackcrystalinfo.tcp;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.EndianUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.util.MiscUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NettyTcpSrv {
	private static Logger logger = LoggerFactory.getLogger(NettyTcpSrv.class);

	public void start(int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			Class ssc = null;
			if (System.getProperties().getProperty("os.name").startsWith("Windows")) {
				System.out.println("Using Nio");
				ssc = NioServerSocketChannel.class;
			} else {
				System.out.println("Using Epoll");
				ssc = EpollServerSocketChannel.class;
			}
			b.group(bossGroup, workerGroup).channel(ssc).childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new TcpSrvDecoder());
					ch.pipeline().addLast(new TcpSrvInHandler());
				}
			}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
			ChannelFuture f = b.bind(port).sync();

			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		NettyTcpSrv server = new NettyTcpSrv();
		server.start(10000);
		System.out.println(System.getProperties().getProperty("os.name"));
	}
}

class TcpSrvDecoder extends ByteToMessageDecoder {
	private final byte[] FRAMEFLAG = MiscUtils.fromHex("fac3c6c9053c3936");
	private static Logger logger = LoggerFactory.getLogger(TcpSrvDecoder.class);

	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		System.out.println("decoder's hashcode: " + this.hashCode());
		int len = in.readableBytes() + in.readerIndex();
		if (len < 8) {
			return;
		}
		in.markReaderIndex();
		byte[] sign = new byte[8];
		in.readBytes(sign);
		System.out.println("sign " + MiscUtils.toHex(sign));
		List<Byte> erbt = new ArrayList<Byte>();
		for (int i = 0; i < 8; i++) {
			if (FRAMEFLAG[i] != sign[i]) {
				erbt.add(sign[i]);
				if (len > in.readerIndex() + 8 + i + 1) {
					byte[] complement = new byte[i + 1];
					in.readBytes(complement);
					System.arraycopy(sign, i + 1, sign, 0, 8 - i - 1);
					System.arraycopy(complement, 0, sign, 8 - i - 1, i + 1);
					i = -1;
					continue;
				} else {
					in.resetReaderIndex();
					return;
				}
			}
		}
		if (len < in.readerIndex() + 2) {
			in.readerIndex(in.readerIndex() - 8);
			return;
		}
		byte[] l = new byte[2];
		in.readBytes(l);
		short dataLength = EndianUtils.readSwappedShort(l, 0);
		if (dataLength < 0) {
			System.out.println("data's length:  	" + dataLength);
			return;
		}

		if (len < in.readerIndex() + dataLength) {
			in.readerIndex(in.readerIndex() - 10);
			return;
		}

		byte[] body = new byte[dataLength];
		in.readBytes(body);
		out.add(body);
		erbt.clear();
	}

}

// class TcpSrvInHandler extends ChannelInboundHandlerAdapter {
// private static Logger logger = LoggerFactory.getLogger(TcpSrvInHandler.class);
//
// @Override
// public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
// logger.info("HelloServerInHandler.channelRead");
// byte[] result = (byte[]) msg;
// System.out.println(MiscUtils.toHex(result));
//
// }
//
// @Override
// public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
// ctx.flush();
// }
// }
class TcpSrvInHandler implements ChannelInboundHandler {
	private static int n = 0;
	private static Logger logger = LoggerFactory.getLogger(TcpSrvInHandler.class);
	private String username;
	private boolean b;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!b) {
			this.username = "岳飞" + this.hashCode();
			b = true;
		} 
		System.out.println(this.username);
		byte[] result = (byte[]) msg;
		logger.info("channelRead {} {} {}", ++n, this.hashCode(),((InetSocketAddress)ctx.channel().remoteAddress()).getHostName());
		ByteBuf bb = Unpooled.buffer(4);
		bb.writeBytes(new byte[]{0x11,0x11,0x11,0x11});
		ctx.writeAndFlush(bb);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelReadComplete");
		ctx.flush();
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		System.out.println("handlerAdded");
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		System.out.println("handlerRemoved");

	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelRegistered");

	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelUnregistered");

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelActive");

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelInactive");

	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		System.out.println("userEventTriggered");

	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelWritabilityChanged");

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		System.out.println("exceptionCaught");

	}
}

class HelloServerInHandler extends ChannelInboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(HelloServerInHandler.class);

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info("HelloServerInHandler.channelRead");
		ByteBuf result = (ByteBuf) msg;
		byte[] result1 = new byte[result.readableBytes()];
		result.readBytes(result1);
		String resultStr = new String(result1);
		System.out.println("Client said:" + resultStr);
		result.release();

		String response = "I am ok!";
		ByteBuf encoded = ctx.alloc().buffer(4 * response.length());
		encoded.writeBytes(response.getBytes());
		ctx.write(encoded);
		ctx.flush();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
}

class HelloClient {
	public void connect(String host, int port) throws Exception {
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			Bootstrap b = new Bootstrap();
			b.group(workerGroup);
			b.channel(NioSocketChannel.class);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new HelloClientIntHandler());
				}
			});

			// Start the client.
			ChannelFuture f = b.connect(host, port).sync();

			// Wait until the connection is closed.
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
		}

	}

	public static void main(String[] args) throws Exception {
		HelloClient client = new HelloClient();
		client.connect("127.0.0.1", 8000);
	}
}

class HelloClientIntHandler extends ChannelInboundHandlerAdapter {
	private static Logger logger = LoggerFactory.getLogger(HelloClientIntHandler.class);

	// 接收server端的消息，并打印出来
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info("HelloClientIntHandler.channelRead");
		ByteBuf result = (ByteBuf) msg;
		byte[] result1 = new byte[result.readableBytes()];
		result.readBytes(result1);
		System.out.println("Server said:" + new String(result1));
		result.release();
	}

	// 连接成功后，向server发送消息
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("HelloClientIntHandler.channelActive");
		String msg = "Are you ok?";
		ByteBuf encoded = ctx.alloc().buffer(4 * msg.length());
		encoded.writeBytes(msg.getBytes());
		ctx.write(encoded);
		ctx.flush();
	}
}