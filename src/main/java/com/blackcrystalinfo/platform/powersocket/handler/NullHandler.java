package com.blackcrystalinfo.platform.powersocket.handler;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;

public class NullHandler implements IHandler {

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		return new Object();
	}

}
