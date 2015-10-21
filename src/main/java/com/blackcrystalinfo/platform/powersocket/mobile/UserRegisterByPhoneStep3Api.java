package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0035;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0040;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0041;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0043;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.PBKDF2;

/**
 * 手机号码注册第三步：入库
 * 
 * @author j
 * 
 */
@Controller("/registerbyphone/step3")
public class UserRegisterByPhoneStep3Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(UserRegisterByPhoneStep3Api.class);

	@Autowired
	ILoginSvr loginSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");
		String step2key = req.getParameter("step2key");
		String password = req.getParameter("password");

		if (StringUtils.isBlank(phone)) {
			ret.put(status, C0035.toString());
			return ret;
		}

		if (StringUtils.isBlank(step2key)) {
			ret.put(status, C0040.toString());
			return ret;
		}

		if (StringUtils.isBlank(password)) {
			ret.put(status, C0043.toString());
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第二步凭证
			String step2keyK = UserRegisterByPhoneStep2Api.STEP2_KEY + phone;
			String step2keyV = jedis.get(step2keyK);
			if (!StringUtils.equals(step2keyV, step2key)) {
				ret.put(status, C0041.toString());
				return ret;
			}

			// 手机号是否已经注册
			boolean exist = loginSvr.userExist(phone);
			if (exist) {
				ret.put(status, ErrorCode.C0036.toString());
				logger.debug("phone has been registed. phone:{}", phone);
				return ret;
			}

			// 注册用户信息
			loginSvr.userRegister(phone, "", phone, "", PBKDF2.encode(password));
			String userId = loginSvr.userGet(User.UserNameColumn, phone).getId();

			ret.put("uID", userId);
			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
