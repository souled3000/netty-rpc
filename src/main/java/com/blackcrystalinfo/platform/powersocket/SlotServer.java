package com.blackcrystalinfo.platform.powersocket;

import java.io.IOException;
import java.util.List;

import com.blackcrystalinfo.platform.App;
import com.blackcrystalinfo.platform.HandlerManager;
import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.exception.HandlerExistedException;
import com.blackcrystalinfo.platform.util.ClzUtils;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.Constants;

public class SlotServer {

	@SuppressWarnings("unchecked")
	public static void bindHandler() throws HandlerExistedException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		List<Class<?>> clzs = ClzUtils.getClasses("com.blackcrystalinfo.platform.powersocket");
		for (Class<?> clz : clzs) {
			if (clz.isAnnotationPresent(Path.class)) {
				Path path = clz.getAnnotation(Path.class);
				Class<IHandler> o = (Class<IHandler>) clz;
				HandlerManager.regHandler(path.path(), o.newInstance());
			}
		}
	}

	public static void main(String[] args) throws Exception {
		CometScanner.tiktok();
		Thread.sleep(3000);
		bindHandler();
		new App(Constants.SERVER_PORT).run();
	}

}
