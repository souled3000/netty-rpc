package com.blackcrystalinfo.platform.powersocket;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.HandlerManager;
import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.examples.App;
import com.blackcrystalinfo.platform.exception.HandlerExistedException;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.Constants;

public class SlotPlatformLauncher {
	private static final Logger logger = LoggerFactory.getLogger(SlotPlatformLauncher.class);

	public static void bindHandler() throws HandlerExistedException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Properties p = new Properties();
		p.load(ClassLoader.getSystemResourceAsStream("mapping.properties"));
		for (Object key : p.keySet()) {
			String className = p.getProperty((String) key);
			@SuppressWarnings("unchecked")
			Class<IHandler> o = (Class<IHandler>) Class.forName(className);
			HandlerManager.regHandler((String) key, o.newInstance());
		}
	}

	public static void main(String[] args) throws Exception {
		logger.info("Start Port {}", Constants.SERVER_PORT);
		CometScanner.tiktok();
		Thread.sleep(3000);
		bindHandler();
		new App(Constants.SERVER_PORT).run();
	}

}
