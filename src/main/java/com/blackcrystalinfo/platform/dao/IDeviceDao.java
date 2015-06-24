package com.blackcrystalinfo.platform.dao;

import com.blackcrystalinfo.platform.powersocket.data.Device;

public interface IDeviceDao {

	/**
	 * 注册设备
	 * 
	 * @param mac
	 *            设备mac
	 * @param sn
	 *            设备序列号
	 * @param name
	 *            设备名称
	 * @param pid
	 *            设备网关
	 * @param dv
	 *            设备类型
	 */
	void regist(byte[] mac, String sn, String name, Long pid, Integer dv);

	String regist(String mac, String sn, String name, String pid, String dv);

	Device get(Long id);

	Device get(byte[] mac);

	boolean exists(byte[] mac);

	/**
	 * 获取设备Id
	 * 
	 * @param mac
	 *            设备mac地址
	 * @return 设备Id
	 */
	Long getIdByMac(byte[] mac);

	String getIdByMac(String mac);

	byte[] getMacById(Long id);

	/**
	 * 修改设备网关
	 * 
	 * @param id
	 *            设备ID
	 * @param pid
	 *            网关设备ID
	 */
	void setPidById(Long id, Long pid);

	void setPidById(String id, String pid);

	/**
	 * 修改设备名称
	 * 
	 * @param id
	 *            设备ID
	 * @param name
	 *            设备名称
	 */
	void setNameById(Long id, String name);

	void setNameById(String id, String name);

}
