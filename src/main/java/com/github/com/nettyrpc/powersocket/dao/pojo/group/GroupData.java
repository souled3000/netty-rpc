package com.github.com.nettyrpc.powersocket.dao.pojo.group;


public class GroupData {

	private String grpName;

	private String grpValue;

	public String getGrpName() {
		return grpName;
	}

	public void setGrpName(String grpName) {
		this.grpName = grpName;
	}

	public String getGrpValue() {
		return grpValue;
	}

	public void setGrpValue(String grpValue) {
		this.grpValue = grpValue;
	}

	@Override
	public String toString() {
		return "GroupData [grpName=" + grpName + ", grpValue=" + grpValue + "]";
	}

}
