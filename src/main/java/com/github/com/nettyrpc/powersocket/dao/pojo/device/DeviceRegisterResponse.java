package com.github.com.nettyrpc.powersocket.dao.pojo.device;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class DeviceRegisterResponse extends ApiResponse {
	private String deviceId;

	private String cookie;

	private String wbKey;

	private String websocketAddr;

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
