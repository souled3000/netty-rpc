package com.github.com.nettyrpc.codec;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class RpcServerInitializer  extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("logger", new LoggingHandler(LogLevel.DEBUG));
        p.addLast("codec-http", new HttpServerCodec());
        p.addLast("aggregator", new HttpObjectAggregator(1024));
        p.addLast("decoder", new HttpRequestDecoder(1024, 1024, 1024, true));
        p.addLast("handler", new RpcCodec());
    }
}

