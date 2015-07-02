package com.blackcrystalinfo.platform.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.blackcrystalinfo.platform.dao.IDeviceDao;
import com.blackcrystalinfo.platform.powersocket.data.Device;

@Repository
public class DeviceDaoImpl implements IDeviceDao {

	private static final Logger logger = LoggerFactory
			.getLogger(DeviceDaoImpl.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void regist(String mac, String sn, String name, Long pid, Integer dv) {
		String sql = "insert into device(mac, sn, name, parentid, device_type_id) values (?,?,?,?,?)";
		jdbcTemplate.update(sql, mac, sn, name, pid, dv);
	}

	@Override
	public Device get(Long id) {
		String sql = "select * from device where id = ?";
		return (Device) jdbcTemplate.queryForObject(sql, new Object[] { id },
				new Device());
	}

	@Override
	public Device get(String mac) {
		String sql = "select * from device where mac = ?";
		return (Device) jdbcTemplate.queryForObject(sql, new Object[] { mac },
				new Device());
	}

	@Override
	public boolean exists(String mac) {
		String sql = "select count(*) from device where mac = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { mac },
				Integer.class) > 0;
	}

	@Override
	public Long getIdByMac(String mac) {
		Long result = null;
		String sql = "select id from device where mac = ?";
		try {
			result = jdbcTemplate.queryForObject(sql, new Object[] { mac },
					Long.class);
		} catch (DataAccessException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("device not exist, mac={}, e={}", mac,
						e.getMessage());
			}
		}
		return result;
	}

	@Override
	public String getMacById(Long id) {
		String sql = "select mac from device where id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id },
				String.class);
	}

	@Override
	public void setPidById(Long id, Long pid) {
		String sql = "update device set pid=? where id=?";
		jdbcTemplate.update(sql, new Object[] { pid, id });
	}

	@Override
	public void setNameById(Long id, String name) {
		String sql = "update device set name=? where id=?";
		jdbcTemplate.update(sql, new Object[] { name, id });
	}

}
