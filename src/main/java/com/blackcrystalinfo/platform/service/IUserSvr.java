package com.blackcrystalinfo.platform.service;

import java.util.List;
import java.util.Map;

import com.blackcrystalinfo.platform.powersocket.bo.User;

public interface IUserSvr {
	List<Map<String, Object>> gotUsers() throws Exception;

	Map<String, Object> saveUser(String name, String mail) throws Exception;

	// for Register API
	boolean userExist(String userName);

	void saveUser(String userName, String phone, String nick, String shadow);
	void saveUser(String userName, String phone, String shadow);
	void userRegister(String userName, String email, String phone, String nick, String shadow);
	void userChangeProperty(String userid, String key, String value);
	void updatePhone(String userid, String phone);
	User getUser(String key, String userName);
}
