package com.blackcrystalinfo.platform.powersocket.dao.pojo.group;

import java.util.List;


public class GroupData {

	private String grpName;

	private List<GroupDevice> grpValue;

	public String getGrpName() {
		return grpName;
	}

	public void setGrpName(String grpName) {
		this.grpName = grpName;
	}

	public List<GroupDevice> getGrpValue() {
		return grpValue;
	}

	public void setGrpValue(List<GroupDevice> grpValue) {
		this.grpValue = grpValue;
	}

	@Override
	public String toString() {
		return "GroupData [grpName=" + grpName + ", grpValue=" + grpValue + "]";
	}
}
