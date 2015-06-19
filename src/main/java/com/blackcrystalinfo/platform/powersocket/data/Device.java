package com.blackcrystalinfo.platform.powersocket.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class Device implements RowMapper<Object> {
	public static final String DeviceIDColumn = "id";
	public static final String DeviceUserIDColumn = "user_id";
	public static final String DeviceNameColumn = "name";
	public static final String DeviceMacColumn = "mac";
	public static final String DeviceSNColumn = "sn";
	public static final String DeviceEncryptKeyColumn = "encryptkey";
	public static final String DeviceRegTimeColumn = "regtime";
	public static final String DeviceParentIDColumn = "parentid";
	public static final String DeviceDeviceTypeColumn = "device_type_id";

	private String id;

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	private String userid;

	public String getUserID() {
		return userid;
	}

	public void setUserID(String userid) {
		this.userid = userid;
	}

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String mac;

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	private String sn;

	public String getSN() {
		return sn;
	}

	public void setSN(String sn) {
		this.sn = sn;
	}

	private String encryptkey;

	public String getEncryptKey() {
		return encryptkey;
	}

	public void setEncryptKey(String encryptkey) {
		this.encryptkey = encryptkey;
	}

	private String regtime;

	public String getRegTime() {
		return regtime;
	}

	public void setRegTime(String regtime) {
		this.regtime = regtime;
	}

	private String parentid;

	public String getParentID() {
		return parentid;
	}

	public void setParentID(String parentid) {
		this.parentid = parentid;
	}

	private String deviceType;

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	@Override
	public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
		Device device = null;
		if (!rs.wasNull()) {
			device = new Device();
			device.setID((rs.getString("id")));
			device.setUserID((rs.getString("user_id")));
			device.setName((rs.getString("name")));
			device.setMac((rs.getString("mac")));
			device.setSN((rs.getString("sn")));
			device.setEncryptKey((rs.getString("encryptkey")));
			device.setRegTime((rs.getString("regtime")));
			device.setParentID((rs.getString("parentid")));
			device.setDeviceType((rs.getString("device_type_id")));
		}
		return device;
	}

	public Device() {

	}

	public Device(String id, String userID, String name, String mac, String sn,
			String encryptkey, String regtime, String parentid,
			String device_type) {
		this.id = id;
		this.userid = userID;
		this.name = name;
		this.mac = mac;
		this.sn = sn;
		this.encryptkey = encryptkey;
		this.regtime = regtime;
		this.parentid = parentid;
		this.deviceType = device_type;
	}
}
