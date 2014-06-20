package com.github.com.nettyrpc.powersocket.dao.pojo.device;

import java.util.List;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class DeviceLoginResponse extends ApiResponse {
	private String proxyKey;

	private String proxyAddr;

	private List<String> bindedUsers;

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

	public List<String> getBindedUsers() {
		return bindedUsers;
	}

	public void setBindedUsers(List<String> bindedUsers) {
		this.bindedUsers = bindedUsers;
	}

}
