package com.blackcrystalinfo.platform.powersocket.api;

import java.util.HashMap;
import java.util.Map;

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
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;

/**
 * 修改绑定手机第二步，确定绑定
 * 
 * @author j
 * 
 */
@Controller("/mobile/phonechange/step2")
public class PhoneChangeStep2Api extends HandlerAdapter {
	private Logger logger = LoggerFactory.getLogger(PhoneChangeStep2Api.class);

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("status", ErrorCode.SYSERROR);

		// 入参解析：cookie， phone
		String cookie = req.getParameter("cookie");
		String phone = req.getParameter("phone");
		String code = req.getParameter("code");

		// phone是否格式正确？用户是否存在？
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		User user = null;
		try {
			user = userDao.userGet(User.UserIDColumn, userId);

			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by id.", e);
			ret.put("status", "用户没找到");
			return ret;
		}

		// 用户已经绑定手机号码？
		String phoneable = user.getPhoneable();
		if ("true".equalsIgnoreCase(phoneable)) {
			ret.put("status", "已绑定手机号码，请勿重复绑定");
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 获取第一步生成的code，未生成或已过期？
			String codeV = jedis.get(PhoneChangeStep1Api.CODE_KEY + phone);
			if (StringUtils.isBlank(codeV)) {
				ret.put("status", "无效验证码，请重新获取");
				return ret;
			}

			// 用户输入的错误？
			if (!StringUtils.equals(code, codeV)) {
				ret.put("status", "验证码错误，请重新输入");
				return ret;
			}

			// 输入无误,清除临时数据
			jedis.del(PhoneChangeStep1Api.CODE_KEY + phone);
			jedis.del(PhoneChangeStep1Api.INTV_KEY + phone);
			jedis.del(PhoneChangeStep1Api.FREQ_KEY + phone);

			// 数据入库
			userDao.userChangeProperty(userId, User.UserPhoneColumn, phone);
			userDao.userChangeProperty(userId, User.UserPhoneableColumn, "true");

			// 返回
			ret.put("status", ErrorCode.SUCCESS);
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
