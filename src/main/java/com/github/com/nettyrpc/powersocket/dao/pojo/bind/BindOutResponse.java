package com.github.com.nettyrpc.powersocket.dao.pojo.bind;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class BindOutResponse extends ApiResponse {
	private String websocketAddr;

	public String getWebsocketAddr() {
		return websocketAddr;
	}

	public void setWebsocketAddr(String websocketAddr) {
		this.websocketAddr = websocketAddr;
	}

}
