package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0036;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0040;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.PBKDF2;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;

/**
 * 手机号码注册第三步：入库
 * 
 * @author j
 * 
 */
@Controller("/rp/3")
public class RegisterStep3Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(RegisterStep3Api.class);

	@Autowired
	IUserSvr usrSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");
		String step2key = req.getParameter("step2key");
		String password = req.getParameter("w");
		if (StringUtils.isBlank(phone)) {
			return ret;
		}
		if (StringUtils.isBlank(step2key)) {
			return ret;
		}
		if (StringUtils.isBlank(password)) {
			return ret;
		}

		if (usrSvr.userExist(phone)) {
			ret.put(status, C0036.toString());
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第二步凭证
			String step2keyK = RegisterStep2Api.STEP2_KEY + phone;
			String step2keyV = jedis.get(step2keyK);
			if (StringUtils.isBlank(step2keyV)) {
				ret.put(status, C0040.toString());
				return ret;
			}
			if (!StringUtils.equals(step2keyV, step2key)) {
				return ret;
			}

			// 手机号是否已经注册
			boolean exist = usrSvr.userExist(phone);
			if (exist) {
				ret.put(status, ErrorCode.C0036.toString());
				logger.debug("phone has been registed. phone:{}", phone);
				return ret;
			}

			// 注册用户信息
			usrSvr.saveUser(phone, phone, PBKDF2.encode(password));
			String userId = usrSvr.getUser(User.UserNameColumn, phone).getId();

			ret.put("uId", userId);
			ret.put(status, SUCCESS.toString());
			SMSSender.send(phone, URLEncoder.encode("注册成功","utf8"));
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
