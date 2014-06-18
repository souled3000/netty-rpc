package com.github.com.nettyrpc.powersocket.dao.pojo.user;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class UserRegisterResponse extends ApiResponse {
	private String userId;

	private String cookie;

	private String wbKey;

	private String websocketAddr;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public String getWbKey() {
		return wbKey;
	}

	public void setWbKey(String wbKey) {
		this.wbKey = wbKey;
	}

	public String getWebsocketAddr() {
		return websocketAddr;
	}

	public void setWebsocketAddr(String websocketAddr) {
		this.websocketAddr = websocketAddr;
	}

}
