package com.blackcrystalinfo.platform;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.blackcrystalinfo.platform.codec.RpcServerInitializer;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);

	@Autowired
	private RpcServerInitializer rpcServerInitializer;

	public void setRpcServerInitializer(RpcServerInitializer rpcServerInitializer) {
		this.rpcServerInitializer = rpcServerInitializer;
	}

	private final int port;

	public App(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup(4);
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(rpcServerInitializer);

			logger.info("start...{}", port);
			Channel ch = b.bind(port).sync().channel();
			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

}
