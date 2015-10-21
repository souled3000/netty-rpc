package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0041;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0042;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
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

import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;

/**
 * 修改绑定手机第二步，校验旧手机发送的短信验证码
 * 
 * @author j
 * 
 */
@Controller("/mobile/phonechange/step2")
public class PhoneChangeStep2Api extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(PhoneChangeStep2Api.class);

	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String STEP2_KEY = "test:tmp:phonechange:step2key:";

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR);

		// 入参解析
		String cookie = req.getParameter("cookie");
		String step1key = req.getParameter("step1key");
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
			ret.put(status, C0006.toString());
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第一步凭证
			String step1keyK = PhoneChangeStep1Api.STEP1_KEY + userId;
			String step1keyV = jedis.get(step1keyK);
			if (!StringUtils.equals(step1keyV, step1key)) {
				ret.put(status, C0041.toString());
				return ret;
			}

			// 获取第一步生成的code，未生成或已过期？
			String codeV = jedis.get(PhoneChangeStep1Api.CODE_KEY + userId);
			if (StringUtils.isBlank(codeV)) {
				ret.put(status, C0042.toString());
				return ret;
			}

			// 用户输入的错误？
			if (!StringUtils.equals(code, codeV)) {
				ret.put(status, C0042.toString());
				return ret;
			}

			// 输入无误,清除临时数据
			jedis.del(PhoneChangeStep1Api.CODE_KEY + userId);
			jedis.del(PhoneChangeStep1Api.INTV_KEY + userId);
			jedis.del(PhoneChangeStep1Api.FREQ_KEY + userId);

			// 生成第二步凭证
			String step2keyK = STEP2_KEY + userId;
			String step2keyV = UUID.randomUUID().toString();
			jedis.setex(step2keyK, CODE_EXPIRE, step2keyV);

			// 返回
			ret.put("step2key", step2keyV);
			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
