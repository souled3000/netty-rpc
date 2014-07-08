package com.github.com.nettyrpc.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.com.nettyrpc.HandlerManager;
import com.github.com.nettyrpc.codec.RpcServerInitializer;
import com.github.com.nettyrpc.exception.HandlerExistedException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);
	
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
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new RpcServerInitializer());

			Channel ch = b.bind(port).sync().channel();
			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void initHandlers() throws HandlerExistedException {
		HandlerManager.regHandler("/reg", new RegHandler());		
	}
	
	public static void main(String[] args) throws Exception {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}
		logger.info("Start Port {}", port);
		initHandlers();
		new App(port).run();
	}
	
}
