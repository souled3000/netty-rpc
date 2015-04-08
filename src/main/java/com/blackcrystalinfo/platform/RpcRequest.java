package com.blackcrystalinfo.platform;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.List;

public class RpcRequest {
	private final String url;
	private final HttpPostRequestDecoder params;
	private final HttpHeaders headers;
	private final String remoteAddr;

	public String getUrl() {
		return url;
	}

	public HttpPostRequestDecoder getParams() {
		return params;
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	/**
	 * 访问的API地址，不包括前缀“/api”及之前的部分
	 * 
	 * @return 访问的API地址
	 */
	public String getUrlOrigin() {
		String result = "";

		if (null == this.url) {
			return result;
		}

		return this.url;
	}

	/**
	 * 
	 * @param url
	 *            请求的地址
	 * @param params
	 *            Post参数
	 * @param headers
	 *            http头信息
	 * @param remoteAddr
	 *            远程地址
	 */
	public RpcRequest(String url, HttpPostRequestDecoder params, HttpHeaders headers, String remoteAddr) {
		this.url = url;
		this.params = params;
		this.headers = headers;
		this.remoteAddr = remoteAddr;
	}

	@Override
	public String toString() {
		if (this.params != null) {
			List<InterfaceHttpData> l = this.params.getBodyHttpDatas();
			StringBuilder r = new StringBuilder();
			for (InterfaceHttpData i : l) {
				try {
					r.append(i.getName()).append(":");
					if (i instanceof Attribute)
						r.append(((Attribute) i).getValue()).append("|");
					else
						r.append("|");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return r.toString();
		} else {
			return "no params";
		}
	}
}
