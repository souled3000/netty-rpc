package com.github.com.nettyrpc.powersocket;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.com.nettyrpc.HandlerManager;
import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.examples.App;
import com.github.com.nettyrpc.exception.HandlerExistedException;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);


	public static void bindHandler() throws HandlerExistedException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		Properties p = new Properties();
		p.load(ClassLoader.getSystemResourceAsStream("mapping.properties"));
		for(Object key : p.keySet()){
			String className = p.getProperty((String)key);
			@SuppressWarnings("unchecked")
			Class<IHandler> o = (Class<IHandler>)Class.forName(className);
			HandlerManager.regHandler((String)key, o.newInstance());
		}
	}
	
	public static void main(String[] args) throws Exception {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}
		logger.info("Start Port {}", port);
		bindHandler();
		new App(port).run();
	}
}
