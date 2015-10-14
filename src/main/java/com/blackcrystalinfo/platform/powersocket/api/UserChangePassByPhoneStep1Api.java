package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.DateUtils;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.VerifyCode;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

/**
 * 
 * 通过手机号码找回用户密码第一步，重发找回密码也调用此接口。
 * 
 * @author j
 * 
 */
@Controller("/changepwdbyphone/step1")
public class UserChangePassByPhoneStep1Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserChangePassByPhoneStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String CODE_KEY = "test:tmp:changepwdbyphone:codekey:";
	public static final String STEP1_KEY = "test:tmp:changepwdbyphone:step1key:";

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			ret.put(status, "手机号码不可以为空");
			return ret;
		}

		User user = null;
		try {
			user = userDao.userGet(User.UserPhoneColumn, phone);

			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by phone.", e);
			ret.put(status, "用户没找到");
			return ret;
		}
		String userId = user.getId();

		Jedis jedis = null;

		try {
			jedis = DataHelper.getJedis();

			// send message
			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put(status, "发送短信验证码失败！");
				return ret;
			}

			String codekey = CODE_KEY + userId;
			String value = jedis.get(codekey);
			if (StringUtils.isNotBlank(value)) {
				ret.put(status, "发送太频繁了，请" + DateUtils.secToTime(CODE_EXPIRE) + "后重试！");
				return ret;
			}

			// 记录短信验证码
			jedis.setex(codekey, CODE_EXPIRE, code);

			// 生成第一步凭证
			String step1keyK = STEP1_KEY + userId;
			String step1keyV = UUID.randomUUID().toString();
			jedis.setex(step1keyK, CODE_EXPIRE, step1keyV);

			ret.put("code", code);
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
