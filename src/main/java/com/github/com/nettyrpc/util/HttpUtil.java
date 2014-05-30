package com.github.com.nettyrpc.util;

import java.io.IOException;

import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;

public class HttpUtil {
	
	public static final String getPostValue(HttpPostRequestDecoder req, String name) {
		try {
			return ((Attribute) req.getBodyHttpData(name)).getValue();
		} catch (NotEnoughDataDecoderException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
