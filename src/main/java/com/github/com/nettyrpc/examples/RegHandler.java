package com.github.com.nettyrpc.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.util.HttpUtil;

public class RegHandler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(RegHandler.class);
	
	public Object rpc(RpcRequest req) throws Exception {
		logger.info("{}", HttpUtil.getPostValue(req.getParams(), "aaa"));
		return new RegInfo("hello", "cx", "1332");
	}

}
