package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C000F;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.PBKDF2;

/**
 * 
 * 通过旧密码修改登录密码第一步，验证旧密码是否正确
 * 
 * @author j
 * 
 */
@Controller("/mobile/changepwd/step1")
public class UserChangePassStep1Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserChangePassStep1Api.class);

	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String STEP1_KEY = "test:tmp:changepwd:step1key:";

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String cookie = req.getParameter("cookie");
		String passOld = req.getParameter("passOld");

		String userId = CookieUtil.gotUserIdFromCookie(cookie);

		User user = null;
		try {
			user = userDao.userGet(User.UserIDColumn, userId);

			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by phone.", e);
			ret.put(status, C0006.toString());
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 用户密码
			String shadow = user.getShadow();

			// 校验密码是否正确
			if (!PBKDF2.validate(passOld, shadow)) {
				ret.put(status, C000F.toString());
				return ret;
			}

			// 生成第一步凭证
			String step1keyK = STEP1_KEY + userId;
			String step1keyV = UUID.randomUUID().toString();
			jedis.setex(step1keyK, CODE_EXPIRE, step1keyV);

			ret.put("step1key", step1keyV);
			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;

	}
}
