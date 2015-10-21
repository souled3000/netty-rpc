package com.blackcrystalinfo.platform.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.blackcrystalinfo.platform.powersocket.bo.User;

@Repository
public class LoginSvrImpl implements ILoginSvr {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Transactional(readOnly = true)
	public List<Map<String, Object>> gotUsers() throws Exception {
		return jdbcTemplate.queryForList("select * from user");
	}

	@Transactional
	public Map<String, Object> saveUser(String name, String mail) throws Exception {
		String sql = "insert into user (name,mail) values (?,?)";
		jdbcTemplate.update(sql, new Object[] { name, mail });
		sql = "select * from user where mail = ?";
		return jdbcTemplate.queryForMap(sql, new Object[] { mail });
	}

	// user
	public boolean userExist(String userName) {
		String sql = "select count(*) from user where " + User.UserNameColumn + "=?";
		return jdbcTemplate.queryForObject(sql, new Object[] { userName.toLowerCase() }, Integer.class) > 0;
	}

	@Transactional
	public void userRegister(String userName, String phone, String nick, String shadow) {
		String sql = "insert into user (" + User.UserNameColumn + "," + User.UserEmailColumn + "," + User.UserPhoneColumn + "," + User.UserNickColumn + "," + User.UserShadowColumn + ") values (?,?,?,?,?)";
		jdbcTemplate.update(sql, new Object[] { userName.toLowerCase(), userName.toLowerCase(), phone, nick, shadow });
	}

	@Transactional
	public void userRegister(String userName, String email, String phone, String nick, String shadow) {
		String sql = "insert into user (" + User.UserNameColumn + "," + User.UserEmailColumn + "," + User.UserPhoneColumn + "," + User.UserNickColumn + "," + User.UserShadowColumn + ") values (?,?,?,?,?)";
		jdbcTemplate.update(sql, new Object[] { userName.toLowerCase(), email.toLowerCase(), phone, nick, shadow });
	}

	public User userGet(String key, String value) {
		String sql = "select * from user where " + key + "=?";
		return (User) jdbcTemplate.queryForObject(sql, new Object[] { value }, new User());
	}

	@Transactional
	public void userChangeProperty(String userid, String key, String value) {
		String sql = "update user set " + key + "=? where " + User.UserIDColumn + "=?";
		jdbcTemplate.update(sql, new Object[] { value, userid });
	}

	// end of user

	public Map<Object, Object> userReg(String userName, String phone, String nick, String shadow) {

		return null;
	}

}
