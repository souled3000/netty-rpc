package com.blackcrystalinfo.platform.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import com.blackcrystalinfo.platform.common.Constants;

@Configuration
public class SlotServer {
	private static final Logger logger = LoggerFactory.getLogger(SlotServer.class);

	@Bean(initMethod = "run")
	public App app() throws InterruptedException {
		CometScanner.tiktok();
		Thread.sleep(3000);
		return new App(Constants.SERVER_PORT);
	}

	public static void main(String[] args) throws Exception {
		String prefix = "";
		final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { prefix + "server.xml", prefix + "beans.xml" });
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				ctx.refresh();
				ctx.close();
				logger.info("stop...{}",Constants.SERVER_PORT);
				super.run();
			}
		});
	}
}
