package com.blackcrystalinfo.platform.common;

public enum LogType {
	ZCDL("注册登录类"),
	ZHAQ("账户安全类"),
	JT("家庭类"),
	GJ("告警类")
	;
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	private String desc;
	private LogType(String code) {
		this.desc=code;
	}
}
