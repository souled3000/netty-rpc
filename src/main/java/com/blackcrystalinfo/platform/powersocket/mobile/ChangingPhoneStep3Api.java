package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0037;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0038;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0044;
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
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;

/**
 * 修改绑定手机第三步，新手机发送短信验证码
 * 
 * @author j
 * 
 */
@Controller("/mobile/cp/3")
public class ChangingPhoneStep3Api extends HandlerAdapter {
	private Logger logger = LoggerFactory.getLogger(ChangingPhoneStep3Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	private static final int DO_FREQ_TTL = Integer.valueOf(Constants.getProperty("phonechange.step3.frequency.ttl", "86400"));

	public static final String FREQ_KEY = "B0034:count:";

	@Autowired
	private IUserSvr userSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR.toString());

		// 入参解析：cookie， phone
		String step2key = req.getParameter("step2key");
		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			return ret;
		}

		if (StringUtils.isBlank(step2key)) {
			return ret;
		}

		String userId = req.getUserId();
		User user = userSvr.getUser(User.UserIDColumn, userId);

		// 新旧手机号一致？
		String oldPhone = user.getPhone();
		if (oldPhone.equals(phone)) {
			ret.put(status, C0044.toString());
			return ret;
		}

		// 手机号是否已经注册
		if (userSvr.userExist(phone)) {
			ret.put(status, ErrorCode.C0036.toString());
			return ret;
		}

		Jedis j = null;
		try {
			j = JedisHelper.getJedis();
			String succ = "cp:succ:"+user.getId();
			if(j.incrBy(succ,0L)>=2){
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}
			// 验证第二步凭证
			if (!j.exists(step2key)) {
				ret.put(status, ErrorCode.C0040.toString());
				return ret;
			}

			Long times = j.incrBy(FREQ_KEY + userId,0);
			if (times >= Constants.USER_COMMON_TIMES) {
				ret.put(status, C002C.toString());
				return ret;
			}

			String operExpir = "B0034:30s:" + user.getId();
			if (j.exists(operExpir)) {
				ret.put(status, C0037.toString());
				return ret;
			}
			j.setex(operExpir, 30, "1");


			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put(status, C0038.toString());
				return ret;
			}

			times=j.incr(FREQ_KEY + userId);
			if (times == 1)
				j.expire(FREQ_KEY + userId, DO_FREQ_TTL);

			String step3keyV = UUID.randomUUID().toString();
			j.setex(step3keyV, CODE_EXPIRE, code + "|" + phone);

			ret.put("count", times);
			ret.put("step3key", step3keyV);
			ret.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.error("", e);
			return ret;
		} finally {
			JedisHelper.returnJedis(j);
		}

		return ret;
	}
}
