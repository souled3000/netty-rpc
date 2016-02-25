package com.blackcrystalinfo.platform.service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;

@Service("rpcServerInitializer")
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {

	@Autowired
	private ApplicationContext ctx;

	private SSLContext context;

	public RpcServerInitializer() {

		try {
			X509TrustManager x509m = new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
			};

			// key store相关信息
			String keyName = "cnkey";
			char[] keyStorePwd = "111111".toCharArray();
			char[] keyPwd = "111111".toCharArray();
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			// 装载当前目录下的key store. 可用jdk中的keytool工具生成keystore
			InputStream in = null;
			keyStore.load(in = RpcServerInitializer.class.getClassLoader().getResourceAsStream(keyName), keyPwd);
			in.close();

			// 初始化key manager factory
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyPwd);

			// 初始化ssl context
			this.context = SSLContext.getInstance("SSL");
			this.context.init(kmf.getKeyManagers(), new TrustManager[] { x509m }, new SecureRandom());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		SSLEngine engine = context.createSSLEngine();
		engine.setUseClientMode(false);
//		p.addFirst("ssl", new SslHandler(engine));
		p.addLast("logger", new LoggingHandler(LogLevel.DEBUG));
		p.addLast("codec-http", new HttpServerCodec());
		p.addLast("aggregator", new HttpObjectAggregator(1024 * 1024));
		p.addLast("decoder", new HttpRequestDecoder(1024, 1024, 1024, true));
		p.addLast("handler", (RpcCodec) ctx.getBean("rpcCodec"));
	}
}
