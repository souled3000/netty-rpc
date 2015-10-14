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
import com.blackcrystalinfo.platform.util.ErrorCode;

/**
 * 
 * 通过手机号码找回用户密码第二步，校验短信验证码是否合法
 * 
 * @author j
 * 
 */
@Controller("/changepwdbyphone/step2")
public class UserChangePassByPhoneStep2Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserChangePassByPhoneStep2Api.class);

	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String STEP2_KEY = "test:tmp:changepwdbyphone:step2key:";

	@Autowired
	private ILoginSvr userDao;

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
			ret.put("status", "手机号码不可以为空");
			return ret;
		}

		if (StringUtils.isBlank(step1key)) {
			ret.put("status", "、第二步凭证不可为空");
			return ret;
		}

		if (StringUtils.isBlank(code)) {
			ret.put("status", "验证码不可为空");
			return ret;
		}

		// 根据手机号获取用户信息
		User user = null;
		String userId = null;
		try {
			user = userDao.userGet(User.UserPhoneColumn, phone);
			if (null == user) {
				throw new Exception("user is null");
			}
			userId = user.getId();
		} catch (Exception e) {
			logger.error("cannot find user by phone.", e);
			ret.put("status", "用户没找到");
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第一步凭证
			String step1keyK = UserChangePassByPhoneStep1Api.STEP1_KEY + userId;
			String step1keyV = jedis.get(step1keyK);
			if (!StringUtils.equals(step1keyV, step1key)) {
				ret.put("status", "通过手机号码找回用户密码第一步凭证有误");
				return ret;
			}

			// 获取第一步生成的code，未生成或已过期？
			String codeV = jedis.get(UserChangePassByPhoneStep1Api.CODE_KEY + userId);
			if (StringUtils.isBlank(codeV)) {
				ret.put("status", "无效验证码，请重新获取");
				return ret;
			}

			// 用户输入的错误？
			if (!StringUtils.equals(code, codeV)) {
				ret.put("status", "验证码错误，请重新输入");
				return ret;
			}

			// 生成第二步凭证
			String step2keyK = STEP2_KEY + userId;
			String step2keyV = UUID.randomUUID().toString();
			jedis.setex(step2keyK, CODE_EXPIRE, step2keyV);

			// 返回
			ret.put("step2key", step2keyV);
			ret.put("status", ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;

	}
}
