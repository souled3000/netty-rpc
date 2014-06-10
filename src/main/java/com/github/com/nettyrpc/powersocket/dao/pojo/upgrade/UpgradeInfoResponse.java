package com.github.com.nettyrpc.powersocket.dao.pojo.upgrade;

import java.util.List;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class UpgradeInfoResponse extends ApiResponse {
	private List<UpgradeData> upgradeDatas;

	public List<UpgradeData> getUpgradeDatas() {
		return upgradeDatas;
	}

	public void setUpgradeDatas(List<UpgradeData> upgradeDatas) {
		this.upgradeDatas = upgradeDatas;
	}

}
