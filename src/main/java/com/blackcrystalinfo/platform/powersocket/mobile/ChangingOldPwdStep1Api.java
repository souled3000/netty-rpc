package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C000F;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.PBKDF2;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

import redis.clients.jedis.Jedis;

/**
 * 
 * 通过旧密码修改登录密码第一步，验证旧密码是否正确
 * 
 * @author j
 * 
 */
@Controller("/mobile/cop/1")
public class ChangingOldPwdStep1Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ChangingOldPwdStep1Api.class);

	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String STEP1_KEY = "test:tmp:changepwd:step1key:";

	@Autowired
	private IUserSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String passOld = req.getParameter("o");

		String userId = req.getUserId();

		User user = null;
		user = userDao.getUser(User.UserIDColumn, userId);

		if (null == user) {
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
