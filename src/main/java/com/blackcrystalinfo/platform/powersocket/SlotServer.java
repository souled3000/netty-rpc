package com.blackcrystalinfo.platform.powersocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.blackcrystalinfo.platform.App;

@Configuration
public class SlotServer {
	private static final Logger logger = LoggerFactory
			.getLogger(SlotServer.class);

	@Bean(initMethod = "run")
	public App app() {
		logger.info("SlotServer app()");
		return new App(8080);
	}

	public static void main(String[] args) throws Exception {
		String prefix = "";
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] { prefix + "server.xml", prefix + "beans.xml" });
		ctx.refresh();

		ctx.close();
	}
}
