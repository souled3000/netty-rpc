package com.blackcrystalinfo.platform.util.mail;

import javax.mail.*;

public class MyAuthenticator extends Authenticator {
	String userName = null;
	String password = null;

	public MyAuthenticator() {
	}

	public MyAuthenticator(String username, String password) {
		this.userName = username;
		this.password = password;
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(userName, password);
	}

	public static void main2(String[] args) {
		// 这个类主要是设置邮件
		MailSenderInfo mailInfo = new MailSenderInfo();
		mailInfo.setMailServerHost("smtp.163.com");
		mailInfo.setMailServerPort("25");
		mailInfo.setValidate(true);
		mailInfo.setUserName("souled3000@163.com");
		mailInfo.setPassword("flower918");// 您的邮箱密码
		mailInfo.setFromAddress("souled3000@163.com");
		mailInfo.setToAddress("souled3000@163.com");
		mailInfo.setSubject("设置邮箱标题");
		mailInfo.setContent("设置邮箱内容");
		// 这个类主要来发送邮件
		SimpleMailSender sms = new SimpleMailSender();
		sms.sendTextMail(mailInfo);// 发送文体格式
		sms.sendHtmlMail(mailInfo);// 发送html格式
	}

	public static void main(String[] args) {
		// 这个类主要是设置邮件
		MailSenderInfo mailInfo = new MailSenderInfo();
		mailInfo.setMailServerHost("smtp.163.com");
		mailInfo.setMailServerPort("25");
		mailInfo.setValidate(true);
		mailInfo.setUserName("souled3000@163.com");
		mailInfo.setPassword("lchjyuefei1981");// 您的邮箱密码
		mailInfo.setFromAddress("souled3000@163.com");
		mailInfo.setToAddress("souled3000@163.com");
		mailInfo.setSubject("设置邮箱标题");
		mailInfo.setContent("数据结构与算法");
		// 这个类主要来发送邮件
		SimpleMailSender sms = new SimpleMailSender();
		sms.sendTextMail(mailInfo);// 发送文体格式
//		sms.sendHtmlMail(mailInfo);// 发送html格式
	}

	public static void main1(String[] args) {
		// 这个类主要是设置邮件
		MailSenderInfo mailInfo = new MailSenderInfo();
		mailInfo.setMailServerHost("smtp.gmail.com");
		mailInfo.setMailServerPort("465");
		mailInfo.setValidate(true);
		mailInfo.setUserName("3000lchj@gmail.com");
		mailInfo.setPassword("66352054");// 您的邮箱密码
		mailInfo.setFromAddress("3000lchj@gmail.com");
		mailInfo.setToAddress("3000lchj@gmail.com");
		mailInfo.setSubject("设置邮箱标题");
		mailInfo.setContent("设置邮箱内容");
		// 这个类主要来发送邮件
		SimpleMailSender sms = new SimpleMailSender();
		sms.sendTextMail(mailInfo);// 发送文体格式
		sms.sendHtmlMail(mailInfo);// 发送html格式
	}
}
