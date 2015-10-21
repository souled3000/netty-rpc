package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

@Controller("/mobile/getUserLog")
public class UserLogApi extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(UserLogApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		// 入参解析：cookie
		String cookie = req.getParameter("cookie");
		String laststamp = req.getParameter("laststamp");

		logger.debug("Get User log start, cookie = {} laststamp = {}", cookie, laststamp);

		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		logger.debug("User id = {}", userId);

		// TODO 根据userId， laststamp获取用户日志
		List<Map<String, String>> userlogs = new ArrayList<Map<String, String>>();
		for (int i = 0; i < 10; i++) {
			Map<String, String> logMap = new HashMap<String, String>();
			logMap.put("logId", "100" + i);
			logMap.put("category", "分类" + i);
			logMap.put("contents", "用户登录" + i);
			logMap.put("createTime", "2015-01-01 12:12:12");
			userlogs.add(logMap);
		}

		ret.put("userlogs", userlogs);
		ret.put(status, ErrorCode.SUCCESS.toString());
		return ret;
	}

}
