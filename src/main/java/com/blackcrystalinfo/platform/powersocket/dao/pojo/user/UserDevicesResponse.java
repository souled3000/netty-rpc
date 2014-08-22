package com.blackcrystalinfo.platform.powersocket.dao.pojo.user;

import java.util.List;

import com.blackcrystalinfo.platform.powersocket.dao.pojo.ApiResponse;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.device.DeviceData;

public class UserDevicesResponse extends ApiResponse {
	private List<DeviceData> bindedDevices;

	public List<DeviceData> getBindedDevices() {
		return bindedDevices;
	}

	public void setBindedDevices(List<DeviceData> bindedDevices) {
		this.bindedDevices = bindedDevices;
	}

}
