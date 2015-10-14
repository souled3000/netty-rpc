package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;

/**
 * 手机号码注册第二步：验证码验证
 * 
 * @author j
 * 
 */
@Controller("/registerbyphone/step2")
public class UserRegisterByPhoneStep2Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserRegisterByPhoneStep2Api.class);
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String STEP2_KEY = "test:tmp:registebyphone:step2key:";

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("status", SYSERROR.toString());

		String phone = req.getParameter("phone");
		String step1key = req.getParameter("step1key");
		String code = req.getParameter("code");

		if (StringUtils.isBlank(phone)) {
			ret.put("status", "手机号码不可以为空");
			return ret;
		}

		if (StringUtils.isBlank(step1key)) {
			ret.put("status", "第一步的凭证不可以为空");
			return ret;
		}

		if (StringUtils.isBlank(code)) {
			ret.put("status", "验证码不可以为空");
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第一步凭证
			String step1keyK = UserRegisterByPhoneStep1Api.STEP1_KEY + phone;
			String step1keyV = jedis.get(step1keyK);
			if (!StringUtils.equals(step1keyV, step1key)) {
				ret.put("status", "注册第一步凭证有误");
				return ret;
			}

			String codekey = "test:tmp:registebyphone:codekey:" + phone;
			String codevalue = jedis.get(codekey);
			if (StringUtils.isBlank(codevalue)) {
				ret.put("status", "请先获取短信验证码");
				return ret;
			}

			if (!StringUtils.equals(codevalue, code)) {
				ret.put("status", "短信验证码输入错误，请重新输入。");
				return ret;
			}

			// 生成第二步凭证
			String step2keyK = STEP2_KEY + phone;
			String step2keyV = UUID.randomUUID().toString();
			jedis.setex(step2keyK, CODE_EXPIRE, step2keyV);

			ret.put("step2key", step2keyV);
			ret.put("status", SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
