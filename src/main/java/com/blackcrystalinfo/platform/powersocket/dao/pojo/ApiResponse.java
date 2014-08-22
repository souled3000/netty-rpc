package com.blackcrystalinfo.platform.powersocket.dao.pojo;

/**
 * 通用API请求响应类
 * 
 * @author j
 * 
 */
public class ApiResponse {

	/**
	 * 执行结果，0为执行成功
	 */
	private int status;

	/**
	 * 执行结果的描述，只有部分status会有描述
	 */
	private String statusMsg;

	/**
	 * 请求API时的URL，只包括url中"/api"部分之后的内容
	 */
	private String urlOrigin;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getStatusMsg() {
		return statusMsg;
	}

	public void setStatusMsg(String statusMsg) {
		this.statusMsg = statusMsg;
	}

	public String getUrlOrigin() {
		return urlOrigin;
	}

	public void setUrlOrigin(String urlOrigin) {
		this.urlOrigin = urlOrigin;
	}

}
