package com.github.com.nettyrpc.exception;

/**
 * 服务器内部错误
 * @author steven
 *
 */
public class InternalException extends Exception {

	private static final long serialVersionUID = 6112878534324677315L;

	public InternalException(String code) {
		super("ErrorCode" + code);
	}
}
