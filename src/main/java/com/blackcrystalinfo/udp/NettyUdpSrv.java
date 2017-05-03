package com.blackcrystalinfo.udp;

import java.net.InetSocketAddress;
import java.util.List;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

public class NettyUdpSrv {

	public static void main(String[] args) throws Exception {
		LogEventMonitor monitor = new LogEventMonitor(new InetSocketAddress(10000));
		try {
			Channel channel = monitor.bind();
			System.out.println("LogEventMonitor running");
			channel.closeFuture().await();
		} finally {
			monitor.stop();
		}
	}
}

class LogEventMonitor {
	private final EventLoopGroup group;
	private final Bootstrap bootstrap;

	public LogEventMonitor(InetSocketAddress address) {
		group = new NioEventLoopGroup();
		bootstrap = new Bootstrap();
		
		bootstrap.group(group).channel(NioDatagramChannel.class).handler(new ChannelInitializer<Channel>() {
//		bootstrap.group(group).channel(EpollDatagramChannel.class).handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				ChannelPipeline pipeline = channel.pipeline();
				// pipeline.addLast(new DevDecoder());
				pipeline.addLast(new DevHandler());
			}
		}).localAddress(address);
	}

	public Channel bind() {
		return bootstrap.bind().syncUninterruptibly().channel();
	}

	public void stop() {
		group.shutdownGracefully();
	}

}

class DevDecoder extends MessageToMessageDecoder<DatagramPacket> {
	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket, List<Object> out) throws Exception {
		ByteBuf data = datagramPacket.content();
		System.out.println(data.readableBytes());
		System.out.println(data.writableBytes());
		System.out.println(data.capacity());
		System.out.println(datagramPacket.recipient().toString());
		System.out.println(datagramPacket.sender().toString());
		System.out.println(data.capacity());
		byte[] b = new byte[data.readableBytes()];
		data.readBytes(b);
		// System.out.println(data.arrayOffset());
		out.add(b);
		System.out.println(this.hashCode());
	}
}

class DevHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) throws Exception {
		ByteBuf data = datagramPacket.content();
		ByteBuf out = data.copy(0, data.readableBytes());
//		System.out.println(data.readableBytes());
//		System.out.println(data.writableBytes());
//		System.out.println(data.capacity());
//		System.out.println(datagramPacket.recipient().toString());
//		System.out.println(datagramPacket.sender().toString());
//		System.out.println(data.capacity());
//		byte[] b = new byte[data.readableBytes()];
//		data.readBytes(b);
//		System.out.println(ctx.channel().remoteAddress().toString());
		System.out.println(datagramPacket.sender().toString());
		ctx.writeAndFlush(new DatagramPacket(out, datagramPacket.sender())).sync();
	}

}
