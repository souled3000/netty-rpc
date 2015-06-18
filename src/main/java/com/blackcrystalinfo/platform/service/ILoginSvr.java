package com.blackcrystalinfo.platform.service;

import java.util.List;
import java.util.Map;

import com.blackcrystalinfo.platform.powersocket.data.User;

public interface ILoginSvr {
	List<Map<String, Object>> gotUsers() throws Exception;

	Map<String, Object> saveUser(String name, String mail) throws Exception;

	// for Register API
	boolean userExist(String userName);

	void userRegister(String userName, String phone, String nick, String shadow);

	void userChangeNick(String userid, String nick);

	User userGet(String key, String userName);
}