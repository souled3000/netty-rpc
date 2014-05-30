package com.github.com.nettyrpc;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;


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
	 * 
	 * @param url 请求的地址
	 * @param params Post参数
	 * @param headers http头信息
	 * @param remoteAddr 远程地址
	 */
	public RpcRequest(String url, HttpPostRequestDecoder params,
			HttpHeaders headers, String remoteAddr) {
		super();
		this.url = url;
		this.params = params;
		this.headers = headers;
		this.remoteAddr = remoteAddr;
	}
}
