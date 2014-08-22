package com.blackcrystalinfo.platform.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.HttpUtil;

public class RegHandler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(RegHandler.class);
	
	public Object rpc(RpcRequest req) throws InternalException {
		logger.info("{}", HttpUtil.getPostValue(req.getParams(), "aaa"));
		return new RegInfo("hello", "cx", "1332");
	}

}
