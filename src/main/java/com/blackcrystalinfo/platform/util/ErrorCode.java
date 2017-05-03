package com.blackcrystalinfo.platform.util;

import java.util.HashMap;
import java.util.Map;

public enum ErrorCode {
	SYSERROR("FFFF"),
	SUCCESS("0000"),
	C0001("0001"),/**cookie中的userId不存在*/
	C0002("0002"),/**cookie验证失败*/
	C0003("0003")/**设备不存在*/,
	C0004("0004")/**设备已被别人绑定*/,
	C0005("0005")/**设备未绑定你,或绑定的不是你*/,
	C0006("0006")/**用户不存在*/,
	C0007("0007")/**家庭已有此用户*/,
	C0008("0008")/**家庭无此用户*/,
	C0009("0009")/**grpOld与grpNew不能都为空*/,
	C000A("000A")/**组增加成功*/,
	C000B("000B")/**组变更成功*/,
	C000C("000C")/**组删除成功*/,
	C000D("000D")/**旧密码为空*/,
	C000E("000E")/**新密码为空*/,
	C000F("000F")/**密码不正确*/,
	C0010("0010")/**找密码1中邮箱为空*/,
	C0011("0011")/**找密码1邮件发送失败*/,

	C0012("0012")/**找密码2中密码参数为空*/,
	C0013("0013")/**找密码2中code为空*/,
	C0014("0014")/**找密码2中邮箱为空*/,
	C0015("0015")/**验证码过期*/,
	C0016("0016")/**超三次验证失败*/,
	C0017("0017")/**验证码不正确*/,
	
	C0018("0018")/**登录中密码为空*/,
	C0019("0019")/**登录中邮箱为空*/,
	C001A("001A")/**用户已退出但家庭还有此用户，结果是用户与家庭已无关系*/,
	C001B("001B")/**用户未退出但家庭已无此用户，结果是用户与家庭已无关系*/,
	C001C("001C")/**用户与家庭已无关系，重复操作*/,
	C001D("001D")/***/,
	C001E("001E")/***/,
	C001F("001F")/***/,
	C0020("0020")/***/,
	C0021("0021")/***/,
	
	C0022("0022")/**注册用户中邮箱为空*/,
	C0023("0023")/**注册用户中密码为空*/,
	C0024("0024")/**注册用户中邮箱已注册*/,
	C0025("0025")/**昵称为空*/,
	C0026("0026")/**昵称没有变化*/,
	
	C0027("0027")/**验证码失败*/,
	C0028("0028")/**用户注册确认已过期*/,
	C0029("0029")/**家庭Id为空*/,
	C002A("002A")/**邮件地址为空*/,
	
	C002B("002B")/**邮箱已经激活*/,
	C002C("002C")/**达到操作上限*/,
	C002D("002D")/**多次请求操作*/,
	;
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	private String code;
	private ErrorCode(String code){
		this.code=code;
	}
	public String toString(){
		return this.code;
	}
	
	public static void main(String[] args) {
		Map<Object,String> m = new HashMap<Object,String>();
		m.put( "HHA",SYSERROR.code);
		
		System.out.println(m.get("HHA"));
		System.out.println(SYSERROR);
	}
}
