package com.github.com.nettyrpc.powersocket.dao.pojo.device;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class DeviceRegisterResponse extends ApiResponse {
	private String deviceId;

	private String cookie;

	private String proxyKey;

	private String proxyAddr;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
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
