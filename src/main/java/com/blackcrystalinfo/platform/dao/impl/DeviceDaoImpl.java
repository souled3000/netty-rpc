package com.blackcrystalinfo.platform.dao.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.blackcrystalinfo.platform.dao.IDeviceDao;
import com.blackcrystalinfo.platform.powersocket.data.Device;
import com.blackcrystalinfo.platform.util.SpringUtils;
import com.blackcrystalinfo.platform.util.StringUtil;

@Repository
public class DeviceDaoImpl implements IDeviceDao {

	private static final Logger logger = LoggerFactory
			.getLogger(DeviceDaoImpl.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	@Transactional
	public void regist(byte[] mac, String sn, String name, Long pid, Integer dv) {
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
	public Device get(byte[] mac) {
		String sql = "select * from device where mac = ?";
		return (Device) jdbcTemplate.queryForObject(sql, new Object[] { mac },
				new Device());
	}

	@Override
	public boolean exists(byte[] mac) {
		String sql = "select count(*) from device where mac = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { mac },
				Integer.class) > 0;
	}

	@Override
	public Long getIdByMac(byte[] mac) {
		String sql = "select id from device where mac = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { mac },
				Long.class);
	}

	@Override
	public byte[] getMacById(Long id) {
		String sql = "select id from device where mac = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id },
				byte[].class);
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

	@Override
	public String getIdByMac(String mac) {
		String result = null;

		try {
			byte[] macByte = StringUtil.mac2Byte(mac);

			Long deviceId = getIdByMac(macByte);

			if (null == deviceId) {
				return result;
			}

			result = deviceId.toString();

		} catch (Exception e) {
			logger.warn("device not exist, mac={}, e={}", mac, e);
		}

		return result;
	}

	@Override
	public String regist(String mac, String sn, String name, String pid,
			String dv) {
		String result = null;

		try {
			Long deviceId = null;
			byte[] macByte = null;
			Long pidLong = null;
			Integer dvInteger = null;

			if (null != mac) {
				macByte = StringUtil.mac2Byte(mac);
			}

			if (null != pid && !"".equals(pid)) {
				pidLong = Long.valueOf(pid);
			}

			if (null != dv && !"".equals(dv)) {
				dvInteger = Integer.valueOf(dv);
			}

			regist(macByte, sn, name, pidLong, dvInteger);

			deviceId = getIdByMac(macByte);

			if (null != deviceId) {
				result = Long.toString(deviceId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public void setNameById(String id, String name) {
		if (null == id) {
			logger.warn("Device id is null");
			return;
		}

		Long idLong = null;

		try {
			idLong = Long.parseLong(id);
		} catch (NumberFormatException e) {
			logger.warn("Device id farmat exception, id:{}", id);
			return;
		}

		setNameById(idLong, name);
	}

	@Override
	public void setPidById(String id, String pid) {
		if (null == id) {
			logger.warn("Device id is null");
			return;
		}

		Long idLong = null;
		Long pidLong = null;
		try {
			idLong = Long.parseLong(id);
			if (null == pid) {
				setPidById(idLong, null);
				return;
			}

			pidLong = Long.parseLong(pid);
			setPidById(idLong, pidLong);
		} catch (NumberFormatException e) {
			logger.warn("Device id farmat exception, id:{}|pid:{}", id, pid);
			return;
		}

	}

	public static void main(String[] args) {
		ApplicationContext ctx = SpringUtils.getCtx();

		IDeviceDao dao = ctx.getBean(IDeviceDao.class);

		String macStr = "KyuqJk6cAAA=";
		try {
			byte[] mac = StringUtil.base64Decode(macStr);
			Device dev = dao.get(mac);

			System.out.println(dev.getBase64Mac());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
