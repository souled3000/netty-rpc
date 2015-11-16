package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0040;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0042;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

/**
 * 修改绑定手机第四步，验证新手机号码，旧手机解绑，新手机绑定
 * 
 * @author j
 * 
 */
@Controller("/mobile/phonechange/step4")
public class PhoneChangeStep4Api extends HandlerAdapter {
	private Logger logger = LoggerFactory.getLogger(PhoneChangeStep4Api.class);

	@Autowired
	private IUserSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR);

		// 入参解析
		String cookie = req.getParameter("cookie");
		String step3key = req.getParameter("step3key");
		String code = req.getParameter("code");

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

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			// 验证第三步凭证
			String step3keyK = PhoneChangeStep3Api.STEP3_KEY + userId;
			String step3keyV = jedis.get(step3keyK);
			if (!StringUtils.equals(step3keyV, step3key)) {
				ret.put(status, C0040.toString());
				return ret;
			}

			String step3phoneK = PhoneChangeStep3Api.STEP3_PHONE + userId;
			String step3phoneV = jedis.get(step3phoneK);

			// 获取第三步生成的code，未生成或已过期？
			String codeV = jedis.get(PhoneChangeStep3Api.CODE_KEY + userId);
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
			jedis.del(PhoneChangeStep3Api.CODE_KEY + userId);
			jedis.del(PhoneChangeStep3Api.INTV_KEY + userId);
			jedis.del(PhoneChangeStep3Api.FREQ_KEY + userId);

			// 数据入库
			userDao.userChangeProperty(userId, User.UserNameColumn, step3phoneV);
			userDao.userChangeProperty(userId, User.UserPhoneColumn, step3phoneV);
			userDao.userChangeProperty(userId, User.UserPhoneableColumn, "true");

			// 返回
			ret.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
