package com.blackcrystalinfo.platform.bk;

import static com.blackcrystalinfo.platform.util.ErrorCode.C000D;
import static com.blackcrystalinfo.platform.util.ErrorCode.C000E;
import static com.blackcrystalinfo.platform.util.ErrorCode.C000F;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0027;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.PBKDF2;

@Path(path = "/mobile/cp")
public class UserChangePassApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserChangePassApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		logger.info("Begin UserChangePass");
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String key = req.getHeaders().get(HttpHeaders.Names.COOKIE);
		String cookie = req.getParameter("cookie");
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		String passOld = req.getParameter("passOld");
		String passNew = req.getParameter("passNew");
		logger.info("UserChangePassHandler begin userId:{}|passOld:{}|passNew:{}", userId, passOld, passNew);

		if (StringUtils.isBlank(passOld)) {
			r.put(status, C000D.toString());
			logger.info("passOld is null. userId:{}|passOld:{}|passNew:{}|status:{}", userId, passOld, passNew, r.get(status));
			return r;
		}
		if (StringUtils.isBlank(passNew)) {
			r.put(status, C000E.toString());
			logger.info("passNew is null. userId:{}|passOld:{}|passNew:{}|status:{}", userId, passOld, passNew, r.get(status));
			return r;
		}

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String flag = j.get("B0010" + key);
			if (Captcha.validity)
				if (flag == null || !flag.equals("succ")) {
					r.put(status, C0027.toString());
					logger.debug("captcha fail.");
					return r;
				}

			// 1. 用户密码
			String shadow = j.hget("user:shadow", userId);

			// 2. 校验密码是否正确
			if (!PBKDF2.validate(passOld, shadow)) {
				r.put(status, C000F.toString());
				logger.info("Password is incorrect. userId:{}|passOld:{}|passNew:{}|status:{}", userId, passOld, passNew, r.get(status));
				return r;
			}

			// 3. 生成新密码
			String newShadow = PBKDF2.encode(passNew);
			j.hset("user:shadow", userId, newShadow);
			j.publish("PubModifiedPasswdUser", userId);

		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			logger.error("User change password error", e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(j);
		}

		logger.info("ronse: userId:{}|passOld:{}|passNew:{}|status:{}", userId, passOld, passNew, r.get(status));
		r.put(status, SUCCESS.toString());
		return r;
	}
}
