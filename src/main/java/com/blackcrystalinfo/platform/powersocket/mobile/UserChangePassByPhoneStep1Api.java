package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0035;
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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
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

	public static final String CODE_KEY = "B0037:codekey:";
	public static final String STEP1_KEY = "B0037:step1key:";

	@Autowired
	private IUserSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			ret.put(status, C0035.toString());
			return ret;
		}

		User user = null;
		try {
			user = userDao.getUser(User.UserPhoneColumn, phone);

			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by phone.", e);
			ret.put(status, C0006.toString());
			return ret;
		}
		String userId = user.getId();

		Jedis jedis = null;

		try {
			jedis = DataHelper.getJedis();

			// send message
			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put(status, C0038.toString());
				return ret;
			}

			String codekey = CODE_KEY + userId;
			String value = jedis.get(codekey);
			if (StringUtils.isNotBlank(value)) {
				ret.put(status, C0037.toString());
				return ret;
			}
			String codekeycount = "B0037:" + userId + ":count";
			int count=0;
			if (jedis.exists(codekeycount)){
				count = Integer.valueOf(jedis.get(codekeycount));
				
				if (count == 6) {
					ret.put(status, C002C.toString());
					return ret;
				}
			}
			
			// 记录短信验证码
			jedis.setex(codekey, CODE_EXPIRE, code);
			if (!jedis.exists(codekeycount))
				jedis.setex(codekeycount, 24 * 60 * 60, "1");
			else
				jedis.incr(codekeycount);
			ret.put("count", count);
			// 生成第一步凭证
			String step1keyK = STEP1_KEY + userId;
			String step1keyV = UUID.randomUUID().toString();
			jedis.setex(step1keyK, CODE_EXPIRE, step1keyV);

			// ret.put("code", code);
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
