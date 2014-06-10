package com.github.com.nettyrpc.powersocket.dao.pojo.group;

import java.util.List;

import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;

public class GroupInfoResponse extends ApiResponse {

	/**
	 * 分组信息列表
	 */
	private List<GroupData> groupDatas;

	public List<GroupData> getGroupDatas() {
		return groupDatas;
	}

	public void setGroupDatas(List<GroupData> groupDatas) {
		this.groupDatas = groupDatas;
	}

}
