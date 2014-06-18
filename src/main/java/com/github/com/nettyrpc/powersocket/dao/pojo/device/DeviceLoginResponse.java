package com.github.com.nettyrpc.powersocket.dao.pojo.device;

import java.util.List;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class DeviceLoginResponse extends ApiResponse {
	private String wbKey;

	private String websocketAddr;

	private List<String> bindedUsers;

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

	public List<String> getBindedUsers() {
		return bindedUsers;
	}

	public void setBindedUsers(List<String> bindedUsers) {
		this.bindedUsers = bindedUsers;
	}

}
