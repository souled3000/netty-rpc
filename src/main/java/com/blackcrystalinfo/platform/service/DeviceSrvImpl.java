package com.blackcrystalinfo.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.blackcrystalinfo.platform.powersocket.bo.Device;

@Repository
public class DeviceSrvImpl implements IDeviceSrv {

	private static final Logger logger = LoggerFactory.getLogger(DeviceSrvImpl.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Transactional
	public void regist(Long id, String mac, String sn, String name, Long pid, Integer dv) {
		String sql = "insert into device(id,mac, sn, name, parentid, device_type_id) values (?,?,?,?,?,?)";
		jdbcTemplate.update(sql,id, mac, sn, name, pid, dv);
	}

	@Transactional
	public Device get(Long id) {
		String sql = "select * from device where id = ?";
		return (Device) jdbcTemplate.queryForObject(sql, new Object[] { id }, new Device());
	}

	@Transactional
	public Device get(String mac) {
		String sql = "select * from device where mac = ?";
		return (Device) jdbcTemplate.queryForObject(sql, new Object[] { mac }, new Device());
	}

	@Transactional
	public boolean exists(String mac) {
		String sql = "select count(*) from device where mac = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { mac }, Integer.class) > 0;
	}

	@Transactional
	public Long getIdByMac(String mac) {
		Long result = null;
		String sql = "select id from device where mac = ?";
		try {
			result = jdbcTemplate.queryForObject(sql, new Object[] { mac }, Long.class);
		} catch (DataAccessException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("device not exist, mac={}, e={}", mac, e.getMessage());
			}
		}
		return result;
	}

	@Transactional
	public String getMacById(Long id) {
		String sql = "select mac from device where id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id }, String.class);
	}

	@Transactional
	public void setPidById(Long id, Long pid) {
		String sql = "update device set pid=? where id=?";
		jdbcTemplate.update(sql, new Object[] { pid, id });
	}

	@Transactional
	public void setNameById(Long id, String name) {
		String sql = "update device set name=? where id=?";
		jdbcTemplate.update(sql, new Object[] { name, id });
	}

}
