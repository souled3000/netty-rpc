package com.blackcrystalinfo.platform.exception;

/**
 * 已经注册Handler错误
 * @author steven
 *
 */
public class HandlerExistedException extends Exception {

	private static final long serialVersionUID = -2614770639556068647L;
	
	public HandlerExistedException(String handlerName) {
		super(handlerName + " is existed");
	}
}
