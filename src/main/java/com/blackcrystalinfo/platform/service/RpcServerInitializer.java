package com.blackcrystalinfo.platform.service;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service("rpcServerInitializer")
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {

	@Autowired
	private ApplicationContext ctx;

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		p.addLast("logger", new LoggingHandler(LogLevel.DEBUG));
		p.addLast("codec-http", new HttpServerCodec());
		p.addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
		p.addLast("decoder", new HttpRequestDecoder(1024, 1024, 1024, true));
		p.addLast("handler", (RpcCodec) ctx.getBean("rpcCodec"));
	}
}
