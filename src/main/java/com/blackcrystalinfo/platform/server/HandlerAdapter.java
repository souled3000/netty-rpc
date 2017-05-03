package com.blackcrystalinfo.platform.server;

import com.alibaba.fastjson.JSONObject;

public abstract class HandlerAdapter implements IHandler {

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		return null;
	}

	@Override
	public Object rpc(JSONObject data) throws Exception {
		return null;
	}

}
