package com.blackcrystalinfo.platform.util.sms;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.httpclient.SimpleHttpClientUtil;

public class SMSSender {

	private static final Logger logger = LoggerFactory.getLogger(SMSSender.class);

	public static boolean send(String phone, String content) {

		logger.info("send SMS: phone={}, content={}", phone, content);
		try {
			String type = "submit";

			StringBuilder sb = new StringBuilder();
			sb.append("type=").append(type);
			sb.append("&phones=").append(phone);
			sb.append("&content=").append(content);

			String ret = SimpleHttpClientUtil.sendGet(Constants.SMS_SENDER_URL, sb.toString());

			logger.info("send ret={}", ret);

			return true;
		} catch (IOException e) {
			logger.error("send short message error!!!", e);
		}

		return false;
	}

	public static void main(String[] args) {
		SMSSender.send("18612455087", "hello_cj");
	}
}
