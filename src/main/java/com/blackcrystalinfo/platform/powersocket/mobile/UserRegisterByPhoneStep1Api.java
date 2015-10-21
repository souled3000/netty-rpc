package com.blackcrystalinfo.platform.powersocket.mobile;

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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

/**
 * 手机号码注册第一步：发送短信验证码
 * 
 * @author j
 * 
 */
@Controller("/registerbyphone/step1")
public class UserRegisterByPhoneStep1Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserRegisterByPhoneStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String STEP1_KEY = "test:tmp:registebyphone:step1key:";

	@Autowired
	private ILoginSvr userDao;

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

			// 判断用户是否已经注册
			boolean exist = userDao.userExist(phone);
			if (exist) {
				ret.put(status, C0036.toString());
				return ret;
			}

			String codekey = "test:tmp:registebyphone:codekey:" + phone;
			String value = jedis.get(codekey);
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

			// 记录短信验证码
			jedis.setex(codekey, CODE_EXPIRE, code);

			// 生成第一步凭证
			String step1keyK = STEP1_KEY + phone;
			String step1keyV = UUID.randomUUID().toString();
			jedis.setex(step1keyK, CODE_EXPIRE, step1keyV);

			//ret.put("code", code);
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
