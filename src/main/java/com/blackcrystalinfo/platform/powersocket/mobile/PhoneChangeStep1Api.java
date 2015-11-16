package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0038;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
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

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;

/**
 * 修改绑定手机号码的第一步，发送短信验证码。
 * 
 * @author j
 * 
 */
@Controller("/mobile/phonechange/step1")
public class PhoneChangeStep1Api extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(PhoneChangeStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	private static final int DO_INTV_TTL = Integer.valueOf(Constants.getProperty("phonechange.step1.interval.ttl", "60"));

	private static final int DO_FREQ_TTL = Integer.valueOf(Constants.getProperty("phonechange.step1.frequency.ttl", "86400"));

	private static final int DO_FREQ_MAX = Integer.valueOf(Constants.getProperty("phonechange.step1.frequency.max", "5"));

	public static final String CODE_KEY = "B0032:step1:";

	public static final String INTV_KEY = "B0032:step1:interval:";

	public static final String FREQ_KEY = "B0032:step1:frequency:";

	public static final String STEP1_KEY = "B0032:step1key:";

	@Autowired
	private IUserSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		// 入参解析：cookie
		String cookie = req.getParameter("cookie");

		// phone是否格式正确？用户是否存在？
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		User user = null;
		try {
			user = userDao.getUser(User.UserIDColumn, userId);

			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by id.", e);
			ret.put(status, C0006.toString());
			return ret;
		}

		// 用户已经绑定手机号码？
		String oldPhone = user.getPhone();

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			// 发送验证码次数是否太频繁，是否超限？
			String frequV = "";

			frequV = j.get(FREQ_KEY + userId);

			if (StringUtils.isNotBlank(frequV)) {
				if (Integer.valueOf(frequV) >= DO_FREQ_MAX) {
					ret.put(status, C002C.toString());
					return ret;
				}
			} else {
				frequV = "0";
			}


			// 生成验证码，服务器端临时存储
			String code = VerifyCode.randString(CODE_LENGTH);
			j.setex(CODE_KEY + userId, CODE_EXPIRE, code);

			// 发送验证码是否成功？
			if (!SMSSender.send(oldPhone, "验证码【" + code + "】")) {
				ret.put(status, C0038.toString());
				return ret;
			}
			;

			// 更新状态记录

			long times = 1L;
			if(j.exists(FREQ_KEY + userId)){
				times = j.incr(FREQ_KEY + userId);
			}else{
				j.setex(FREQ_KEY + userId, DO_FREQ_TTL, "1");
			}

			// 生成第一步凭证
			String step1keyK = STEP1_KEY + userId;
			String step1keyV = UUID.randomUUID().toString();
			j.setex(step1keyK, CODE_EXPIRE, step1keyV);

			// 返回
			// ret.put("code", code);
			ret.put("count", times);
			ret.put("step1key", step1keyV);
			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(j);
		}

		return ret;
	}
}
