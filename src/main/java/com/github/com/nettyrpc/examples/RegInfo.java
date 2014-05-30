package com.github.com.nettyrpc.examples;

public class RegInfo {
	private final String email;
	private final String pass;
	private final String phone;
	
	public RegInfo(String email, String pass, String phone) {
		super();
		this.email = email;
		this.pass = pass;
		this.phone = phone;
	}
	public String getEmail() {
		return email;
	}
	public String getPass() {
		return pass;
	}
	public String getPhone() {
		return phone;
	}
}
