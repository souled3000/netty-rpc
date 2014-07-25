package com.github.com.nettyrpc.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurableConstants {

	protected static final Logger logger = LoggerFactory.getLogger(ConfigurableConstants.class);

	protected static final Properties p = new Properties();

	protected static void init(String propertyFileName) {
		logger.info("init: {}", propertyFileName);
		InputStream is = null;
		try {
			is = ClassLoader.getSystemResourceAsStream(propertyFileName);
			if (null != is) {
				p.load(is);
			}
		} catch (IOException e) {
			logger.error("load property file: {} error!", propertyFileName);
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("close " + propertyFileName + " error");
				}
			}
		}
	}

	
}
