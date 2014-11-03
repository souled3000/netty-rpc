package com.blackcrystalinfo.platform.powersocket;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.HandlerManager;
import com.blackcrystalinfo.platform.examples.App;
import com.blackcrystalinfo.platform.exception.HandlerExistedException;
import com.blackcrystalinfo.platform.powersocket.handler.GeLiWebsocketHandler;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.Constants;

public class GeLiSrv {

	private static final Logger logger = LoggerFactory.getLogger(SlotPlatformLauncher.class);

	public static void bindHandler() throws HandlerExistedException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		HandlerManager.regHandler("/api/geli/getUrl", new GeLiWebsocketHandler());
	}

	public static void main(String[] args) throws Exception {
		logger.info("Start Port {}", Constants.SERVER_PORT);
		Thread t = t = new Thread(new Runnable(){
			@Override
			public void run() {
//				CometScannerV2.scan();
				CometScanner.tiktok();
			}
		});
		t.start();
		bindHandler();
		new App(Constants.SERVER_PORT).run();
	}

}
