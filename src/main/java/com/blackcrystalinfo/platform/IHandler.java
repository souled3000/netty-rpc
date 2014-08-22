package com.blackcrystalinfo.platform;

import com.blackcrystalinfo.platform.exception.InternalException;

public interface IHandler {
	public Object rpc(RpcRequest req) throws InternalException;
}
