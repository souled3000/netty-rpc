package com.blackcrystalinfo.platform.util;

import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;

import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.cryto.Datagram;

public class HttpUtil {

	public static final String getPostValue(HttpPostRequestDecoder req, String name) {
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

	public static final Datagram getDatagram(RpcRequest req) {
		String ktm = HttpUtil.getPostValue(req.getParams(), "ktm");
		String crc = HttpUtil.getPostValue(req.getParams(), "crc");
		String ctn = HttpUtil.getPostValue(req.getParams(), "ctn");
		ctn = ctn.replaceAll(" ", "+");
		String ctp = HttpUtil.getPostValue(req.getParams(), "ctp");
		String key = HttpUtil.getPostValue(req.getParams(), "key");

		Datagram data = null;
		try {
			data = new Datagram(key, ktm, ctp, crc, ctn);
			data.decapsulation();
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		System.out.println("------------------------------------------" + data.getCtn());
		return data;
	}
}
