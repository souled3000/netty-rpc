package com.blackcrystalinfo.platform;

import com.alibaba.fastjson.JSONObject;

public interface IHandler {
	public Object rpc(RpcRequest req) throws Exception;

	public Object rpc(JSONObject data) throws Exception;
}
