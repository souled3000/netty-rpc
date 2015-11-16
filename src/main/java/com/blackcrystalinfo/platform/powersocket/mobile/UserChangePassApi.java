package com.blackcrystalinfo.platform.powersocket.mobile;

import io.netty.handler.codec.http.HttpHeaders;

import static com.blackcrystalinfo.platform.common.ErrorCode.C000D;
import static com.blackcrystalinfo.platform.common.ErrorCode.C000E;
import static com.blackcrystalinfo.platform.common.ErrorCode.C000F;
import static com.blackcrystalinfo.platform.common.ErrorCode.C001D;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0027;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002A;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.PBKDF2;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.service.InternalException;

/**
 * 修改密码
 * 
 * @author Shenjz
 */
@Controller("/cp")
public class UserChangePassApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserChangePassApi.class);

	@Autowired
	IUserSvr loginSvr;

	private boolean valid(String userEmail, String passOld, String passNew, Map<Object, Object> r) {
		if (StringUtils.isBlank(userEmail)) {
			r.put(status, C002A.toString());
			logger.info("userEmail is null. userEmail:{}|passOld:{}|passNew:{}|status:{}", userEmail, passOld, passNew, r.get(status));
			return false;
		}
		if (StringUtils.isBlank(passOld)) {
			r.put(status, C000D.toString());
			logger.info("passOld is null. userEmail:{}|passOld:{}|passNew:{}|status:{}", userEmail, passOld, passNew, r.get(status));
			return false;
		}
		if (StringUtils.isBlank(passNew)) {
			r.put(status, C000E.toString());
			logger.info("passNew is null. userEmail:{}|passOld:{}|passNew:{}|status:{}", userEmail, passOld, passNew, r.get(status));
			return false;
		}
		return true;
	}

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		logger.info("Begin UserChangePass");
		Map<Object, Object> r = new HashMap<Object, Object>();

		String key = req.getHeaders().get(HttpHeaders.Names.COOKIE);
		String userEmail = req.getParameter("email");
		String passOld = req.getParameter("passOld");
		String passNew = req.getParameter("passNew");
		logger.info("UserChangePassHandler begin userEmail:{}|passOld:{}|passNew:{}", userEmail, passOld, passNew);

		if (!valid(userEmail, passOld, passNew, r)) {
			return r;
		}

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			String flag = j.get("B0010" + key);
			if (Captcha.validity)
				if (flag == null) {
					r.put(status, C001D.toString());
					logger.debug("captcha fail.");
					return r;
				} else if (!flag.equals("succ")) {
					r.put(status, C0027.toString());
					logger.debug("captcha fail.");
					return r;
				}

			// 0. 根据用户邮箱，查找用户ID
			User user = loginSvr.getUser(User.UserNameColumn, userEmail);

			String userId = user.getId();

			// 登陆名称验证
			if (StringUtils.isEmpty(userId)) {
				logger.debug("unkonw email{}", userEmail);
				r.put(status, ErrorCode.C0020.toString());
				return r;
			}

			// 修改密码次数
			int times = 0;
			String strTimes = j.get("user:passwdChangedTimes:" + userId);
			Long ttl = j.ttl("user:passwdChangedTimes:" + userId);
			if (StringUtils.isNotBlank(strTimes)) {
				times = Integer.valueOf(strTimes);
			}

			// 次数达到上限
			if (times >= Constants.PASSWD_CHANGED_TIMES_MAX) {
				logger.warn("Change passwd too many times");
				r.put("passwdChangedTimes", times);
				r.put("ttl", ttl);
				r.put(status, ErrorCode.C002F.toString());
				return r;
			}

			// 1. 用户密码
			String shadow = user.getShadow();

			// 2. 校验密码是否正确
			if (!PBKDF2.validate(passOld, shadow)) {
				r.put(status, C000F.toString());
				logger.info("Password is incorrect. userId:{}|passOld:{}|passNew:{}|status:{}", userId, passOld, passNew, r.get(status));

				/**
				 * 密码不正确，用户可能走找回密码流程，passOld可能是临时密码.
				 */
				// 验证码是否过期
				String keyCode = new String(userEmail + "-code");
				if (!j.exists(keyCode)) {
					r.put(status, ErrorCode.C0015.toString());
					logger.info("code has been expired. userEmail:{}|keyCode:{}|status:{}", userEmail, keyCode, r.get(status));
					return r;
				}

				// 超三次验证失败直接返回
				String codeVal = j.get(keyCode);
				String strFailTime = j.get(keyCode + "fail");
				int failTime = Integer.valueOf(strFailTime == null ? "0" : strFailTime);
				if (failTime >= 3) {
					r.put(status, ErrorCode.C0016.toString());
					logger.info("the times validating beyond three. userEmail:{}|keyCode:{}|status:{}", userEmail, keyCode, r.get(status));
					return r;
				}

				// 验证
				if (!codeVal.equals(passOld)) {
					r.put(status, ErrorCode.C0017.toString());
					j.incr(keyCode + "fail");// 累记失败次数
					logger.info("validating fail. userEmail:{}|keyCode:{}|status:{}", userEmail, keyCode, r.get(status));
					return r;
				}
			}

			// 3. 生成新密码
			String newShadow = PBKDF2.encode(passNew);
			// j.hset("user:shadow", userId, newShadow);
			loginSvr.userChangeProperty(userId, User.UserShadowColumn, newShadow);
			j.publish("PubModifiedPasswdUser", userId);

			// 4. 修改密码有次数限制：24小时内只能成功改两次
			times++;
			j.setex("user:passwdChangedTimes:" + userId, Constants.PASSWD_CHANGED_EXPIRE, String.valueOf(times));
			r.put("passwdChangedTimes", times);

			// bug37: 既然修改成功了，就应该把重发激活邮件次数清空了
			j.del("user:findpwdtimes:" + userId);

			r.put(status, SUCCESS.toString());
			logger.info("ronse: userEmail:{}|passOld:{}|passNew:{}|status:{}", userEmail, passOld, passNew, r.get(status));
		} catch (Exception e) {
			r.put(status, SYSERROR.toString());
			logger.error("User change password error", e);
			throw new InternalException(e.getMessage());
		} finally {
			DataHelper.returnJedis(j);
		}

		return r;
	}
}
