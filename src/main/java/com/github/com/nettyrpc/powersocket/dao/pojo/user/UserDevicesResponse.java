package com.github.com.nettyrpc.powersocket.dao.pojo.user;

import java.util.List;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class UserDevicesResponse extends ApiResponse {
	private List<String> bindedDevices;

	public List<String> getBindedDevices() {
		return bindedDevices;
	}

	public void setBindedDevices(List<String> bindedDevices) {
		this.bindedDevices = bindedDevices;
	}

}
