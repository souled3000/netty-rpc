package com.blackcrystalinfo.platform.powersocket.data;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.PBKDF2;

public class User implements RowMapper<Object> {
	public static final String UserIDColumn = "id";
	public static final String UserNameColumn = "username";
	public static final String UserEmailColumn = "email";
	public static final String UserPhoneColumn = "phone";
	public static final String UserNickColumn = "nick";
	public static final String UserShadowColumn = "shadow";
	public static final String UserEmailableColumn = "emailable";
	public static final String UserAdminidColumn = "adminid";
	public static final String UserPhoneableColumn = "phoneable";

	@Override
	public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
		User user = null;
		if (!rs.wasNull()) {
			user = new User();

			user.setId((rs.getString(UserIDColumn)));
			user.setEmail((rs.getString(UserEmailColumn)));
			user.setUserName((rs.getString(UserNameColumn)));
			user.setPhone((rs.getString(UserPhoneColumn)));
			user.setNick((rs.getString(UserNickColumn)));
			user.setShadow((rs.getString(UserShadowColumn)));
			user.setEmailable((rs.getString(UserEmailableColumn)));
			user.setPhoneable(rs.getString(UserPhoneableColumn));
		}

		// user.setAdminid(UserAdminidColumn);
		return user;
	}

	public User() {

	}

	private String id;
	private String userName;
	private String email;
	private String phone;
	private String nick;
	private String shadow;
	private String emailable;
	private String adminid;
	private String phoneable;

	public String getUserName() {
		return userName;
	}

	public boolean validate(String pwd) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		return PBKDF2.validate(pwd, shadow);
	}

	public String getCookie() throws NoSuchAlgorithmException {
		return CookieUtil.encode4user(id, CookieUtil.EXPIRE_SEC, shadow);
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public String getShadow() {
		return shadow;
	}

	public void setShadow(String shadow) {
		this.shadow = shadow;
	}

	public String getEmailable() {
		return emailable;
	}

	public String getAbleEmail() {
		if ("true".equalsIgnoreCase(emailable)) {
			return email;
		} else {
			return "";
		}
	}

	public void setEmailable(String emailable) {
		this.emailable = emailable;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAdminid() {
		if (StringUtils.isBlank(adminid))
			adminid = "";
		return adminid;
	}

	public void setAdminid(String adminid) {
		this.adminid = adminid;
	}

	public String getPhoneable() {
		return phoneable;
	}

	public void setPhoneable(String phoneable) {
		this.phoneable = phoneable;
	}

}
