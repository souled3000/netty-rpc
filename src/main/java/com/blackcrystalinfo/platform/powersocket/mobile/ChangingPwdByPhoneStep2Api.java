package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
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
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

import redis.clients.jedis.Jedis;

/**
 * 
 * 通过手机号码找回用户密码第二步，校验短信验证码是否合法
 * 
 * @author j
 * 
 */
@Controller("/cpp/2")
public class ChangingPwdByPhoneStep2Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ChangingPwdByPhoneStep2Api.class);

	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Autowired
	private IUserSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		// 解析参数
		String phone = req.getParameter("phone");
		String step1key = req.getParameter("step1key");
		String code = req.getParameter("code");

		// 校验参数
		if (StringUtils.isBlank(phone)) {
			return ret;
		}

		if (StringUtils.isBlank(step1key)) {
			return ret;
		}

		if (StringUtils.isBlank(code)) {
			return ret;
		}

		// 根据手机号获取用户信息
		User user = userDao.getUser(User.UserPhoneColumn, phone);
		if (null == user) {
			ret.put(status, C0006.toString());
			return ret;
		}
		Jedis jedis = null;
		try {
			jedis = JedisHelper.getJedis();

			if (jedis.incrBy("B0037:succ:" + user.getId(), 0L) >= 2) {
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}

			String codeV = jedis.get(step1key+phone);
			if (!StringUtils.equals(code, codeV)) {
				ret.put(status, ErrorCode.C0042.toString());
				return ret;
			}
			if (StringUtils.isBlank(codeV)) {
				ret.put(status, ErrorCode.C0040.toString());
				return ret;
			}
			jedis.del(step1key);

			ret.put("step2key", UUID.randomUUID().toString());
			jedis.setex((String) ret.get("step2key"), CODE_EXPIRE, "");

			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			JedisHelper.returnJedis(jedis);
		}

		return ret;

	}
}
