package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0035;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0036;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0037;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0038;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;

/**
 * 手机号码注册第一步：发送短信验证码
 * 
 * @author j
 * 
 */
@Controller("/rp/1")
public class RegisterStep1Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(RegisterStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Autowired
	private IUserSvr usrSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			ret.put(status, C0035.toString());
			return ret;
		}

		Jedis j = null;

		try {
			j = DataHelper.getJedis();

			if (usrSvr.userExist(phone)) {
				ret.put(status, C0036.toString());
				return ret;
			}
			String freq = "B0029:30s:" + phone;
			if (StringUtils.isNotBlank(j.get(freq))) {
				ret.put(status, C0037.toString());
				return ret;
			}
			j.setex(freq,30,"");

			String invokeCount = "B0029:count:" + phone;
			long times = j.incrBy(invokeCount,0);
			if (times >= Constants.USER_COMMON_TIMES) {
				ret.put(status, C002C.toString());
				return ret;
			}
			
			// send message
			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put(status, C0038.toString());
				return ret;
			}
			
			times=j.incr(invokeCount);
			if (times == 1) {
				j.expire(invokeCount, 24 * 60 * 60);
			}
			ret.put("count", times);

			String key = UUID.randomUUID().toString();
			j.setex(key, CODE_EXPIRE, code);

			ret.put("step1key", key);
			ret.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			DataHelper.returnJedis(j);
		}

		return ret;
	}
}
