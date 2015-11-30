package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

@Controller("/mobile/logout")
public class UserLogout extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(UserLogout.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		// 入参解析：cookie， phone
		String cookie = req.getParameter("cookie");

		logger.debug("Get User logout start, cookie = {} ", cookie);

		String userId = req.getUserId();
		logger.debug("User id = {}", userId);

		ret.put(status, SUCCESS.toString());
		return ret;
	}

}
