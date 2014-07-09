package com.github.com.nettyrpc.powersocket.dao.pojo.user;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class UserRegisterResponse extends ApiResponse {
	private String userId;

	private String cookie;

	private String proxyKey;

	private String proxyAddr;
	
	private String expire;

	public String getExpire() {
		return expire;
	}

	public void setExpire(String expire) {
		this.expire = expire;
	}

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

	public String getProxyKey() {
		return proxyKey;
	}

	public void setProxyKey(String proxyKey) {
		this.proxyKey = proxyKey;
	}

	public String getProxyAddr() {
		return proxyAddr;
	}

	public void setProxyAddr(String proxyAddr) {
		this.proxyAddr = proxyAddr;
	}

}
