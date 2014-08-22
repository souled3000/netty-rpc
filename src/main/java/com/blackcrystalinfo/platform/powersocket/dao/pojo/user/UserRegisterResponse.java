package com.blackcrystalinfo.platform.powersocket.dao.pojo.user;

import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;

public class UserRegisterResponse extends ApiResponse {
	private String userId;

	private String cookie;

	private String proxyKey;

	private String proxyAddr;
	
	private String heartBeat;


	public String getHeartBeat() {
		return heartBeat;
	}

	public void setHeartBeat(String heartBeat) {
		this.heartBeat = heartBeat;
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
