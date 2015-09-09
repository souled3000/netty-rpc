package com.blackcrystalinfo.platform;

import java.util.LinkedHashMap;
import java.util.Map;

import com.blackcrystalinfo.platform.exception.HandlerExistedException;

/**
 * 应用注册器
 * 
 * @author steven
 * 
 */
public class HandlerManager {
	private static Map<String, IHandler> handlers = new LinkedHashMap<String, IHandler>();

	/**
	 * 注册API
	 * 
	 * @param regURL
	 *            请求地址
	 * @param handler
	 *            具体的实现类
	 * @throws HandlerExistedException
	 *             已经存在报错
	 */
	public static final void regHandler(String regURL, IHandler handler) throws HandlerExistedException {
		IHandler h = handlers.get(regURL);
		if (h != null) {
			throw new HandlerExistedException(regURL);
		}
		handlers.put(regURL, handler);
	}

	/**
	 * 获取API
	 * 
	 * @param regURL
	 *            请求地址
	 * @return 具体的类
	 */
	public static final IHandler getHandler(String regURL) {
		IHandler h = handlers.get(regURL);
		return h;
	}
}
