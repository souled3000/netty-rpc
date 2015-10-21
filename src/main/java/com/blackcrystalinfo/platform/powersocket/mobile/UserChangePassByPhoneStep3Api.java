package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
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
import com.blackcrystalinfo.platform.util.PBKDF2;

/**
 * 
 * 通过手机号码找回用户密码第三步，执行修改密码操作
 * 
 * @author j
 * 
 */
@Controller("/changepwdbyphone/step3")
public class UserChangePassByPhoneStep3Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserChangePassByPhoneStep3Api.class);

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		// 解析参数
		String phone = req.getParameter("phone");
		String step2key = req.getParameter("step2key");
		String password = req.getParameter("password");

		// 校验参数
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
			ret.put(status, C0006.toString());
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第二步凭证
			String step2keyK = UserChangePassByPhoneStep2Api.STEP2_KEY + userId;
			String step2keyV = jedis.get(step2keyK);
			if (!StringUtils.equals(step2keyV, step2key)) {
				ret.put(status, C0041.toString());
				return ret;
			}

			// 生成新密码
			String newShadow = PBKDF2.encode(password);
			userDao.userChangeProperty(userId, User.UserShadowColumn, newShadow);
			jedis.publish("PubModifiedPasswdUser", userId);

			// 返回
			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.error("reg by phone step1 error! ", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
