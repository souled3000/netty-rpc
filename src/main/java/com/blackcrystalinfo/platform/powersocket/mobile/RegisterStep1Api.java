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
	private static final int DO_FREQ_MAX = Integer.valueOf(Constants.getProperty("phonechange.step1.frequency.max", "5"));

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

		Jedis jedis = null;

		try {
			jedis = DataHelper.getJedis();

			if (usrSvr.userExist(phone)) {
				ret.put(status, C0036.toString());
				return ret;
			}

			String frequency = "B0029:" + phone + ":frequency";
			String daily = "B0029:" + phone + ":daily";
			int count = 1;
			boolean b = jedis.exists(daily);
			if (b) {
				count = Integer.valueOf(jedis.get(daily));
				if (count >= DO_FREQ_MAX) {
					ret.put(status, C002C.toString());
					return ret;
				}
			}

			String value = jedis.get(frequency);
			if (StringUtils.isNotBlank(value)) {
				ret.put(status, C0037.toString());
				return ret;
			}

			// send message
			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put(status, C0038.toString());
				return ret;
			}

			if (!b) {
				jedis.setex(daily, 24 * 60 * 60, count+"");
			} else {
				count=jedis.incr(daily).intValue();
			}
			if (!jedis.exists(frequency)) {
				jedis.setex(frequency, 30, "1");
			}
			ret.put("count", count);

			String step1keyV = UUID.randomUUID().toString();
			jedis.setex(step1keyV, CODE_EXPIRE, code);

			ret.put("step1key", step1keyV);
			ret.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
