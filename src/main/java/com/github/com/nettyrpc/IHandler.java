package com.github.com.nettyrpc;

import com.github.com.nettyrpc.exception.InternalException;

public interface IHandler {
	public Object rpc(RpcRequest req) throws InternalException;
}
