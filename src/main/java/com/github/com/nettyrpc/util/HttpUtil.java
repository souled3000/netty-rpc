package com.github.com.nettyrpc.util;

import java.io.IOException;

import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

public class HttpUtil {

	public static final String getPostValue(HttpPostRequestDecoder req,
			String name) {
		try {
			InterfaceHttpData bodyHttpData = req.getBodyHttpData(name);
			if (null != bodyHttpData) {
				return ((Attribute) bodyHttpData).getValue();
			}
			return "";
		} catch (NotEnoughDataDecoderException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
}
