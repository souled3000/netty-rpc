package com.github.com.nettyrpc.powersocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.com.nettyrpc.HandlerManager;
import com.github.com.nettyrpc.codec.RpcServerInitializer;
import com.github.com.nettyrpc.examples.App;
import com.github.com.nettyrpc.exception.HandlerExistedException;
import com.github.com.nettyrpc.powersocket.handler.BindInHandler;
import com.github.com.nettyrpc.powersocket.handler.BindOutHandler;
import com.github.com.nettyrpc.powersocket.handler.DeviceLoginHandler;
import com.github.com.nettyrpc.powersocket.handler.DeviceRegisterHandler;
import com.github.com.nettyrpc.powersocket.handler.GroupInfoHandler;
import com.github.com.nettyrpc.powersocket.handler.GroupUploadHandler;
import com.github.com.nettyrpc.powersocket.handler.UpgradeInfoHandler;
import com.github.com.nettyrpc.powersocket.handler.UpgradeUploadHandler;
import com.github.com.nettyrpc.powersocket.handler.UserDevicesHandler;
import com.github.com.nettyrpc.powersocket.handler.UserLoginHandler;
import com.github.com.nettyrpc.powersocket.handler.UserRegisterHandler;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private final int port;

	public Main(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup(4);
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_BACKLOG, 1024);
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
		HandlerManager.regHandler("/api/deviceGrouping/upload", new GroupUploadHandler());
		HandlerManager.regHandler("/api/deviceGrouping/info", new GroupInfoHandler());
		HandlerManager.regHandler("/api/softwareUpgrading/upgradeUpload", new UpgradeUploadHandler());
		HandlerManager.regHandler("/api/softwareUpgrading/upgradeInfo", new UpgradeInfoHandler());
		HandlerManager.regHandler("/api/user/register", new UserRegisterHandler());
		HandlerManager.regHandler("/api/user/login", new UserLoginHandler());
		HandlerManager.regHandler("/api/user/devices", new UserDevicesHandler());
		HandlerManager.regHandler("/api/device/register", new DeviceRegisterHandler());
		HandlerManager.regHandler("/api/device/login", new DeviceLoginHandler());
		HandlerManager.regHandler("/api/bind/in", new BindInHandler());
		HandlerManager.regHandler("/api/bind/out", new BindOutHandler());
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
