package com.blackcrystalinfo.platform;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.exception.InternalException;

public interface IHandler {
	public Object rpc(RpcRequest req) throws Exception;

	public Object rpc(JSONObject data) throws Exception;
}
