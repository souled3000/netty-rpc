package com.blackcrystalinfo.platform.powersocket.handler;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;

public class NullHandler extends HandlerAdapter {

	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		return new Object();
	}

}
