package com.github.com.nettyrpc;

public interface IHandler {
	public Object rpc(RpcRequest req) throws Exception;
}
