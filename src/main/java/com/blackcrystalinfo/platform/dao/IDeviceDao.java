package com.blackcrystalinfo.platform.dao;

import com.blackcrystalinfo.platform.powersocket.data.Device;

/**
 * 设备数据库访问接口
 * 
 * @author j
 * 
 */
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
	void regist(String mac, String sn, String name, Long pid, Integer dv);

	/**
	 * 获取设备
	 * 
	 * @param id
	 *            设备ID
	 * @return
	 */
	Device get(Long id);

	/**
	 * 获取设备
	 * 
	 * @param mac
	 *            设备mac地址
	 * @return
	 */
	Device get(String mac);

	/**
	 * 设备是否存在
	 * 
	 * @param mac
	 *            设备mac地址
	 * @return
	 */
	boolean exists(String mac);

	/**
	 * 获取设备Id
	 * 
	 * @param mac
	 *            设备mac地址
	 * @return 设备Id
	 */
	Long getIdByMac(String mac);

	/**
	 * 获取设备mac
	 * 
	 * @param id
	 *            设备ID
	 * @return 设备mac地址
	 */
	String getMacById(Long id);

	/**
	 * 修改设备网关
	 * 
	 * @param id
	 *            设备ID
	 * @param pid
	 *            网关设备ID
	 */
	void setPidById(Long id, Long pid);

	/**
	 * 修改设备名称
	 * 
	 * @param id
	 *            设备ID
	 * @param name
	 *            设备名称
	 */
	void setNameById(Long id, String name);

}
