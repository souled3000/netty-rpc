package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
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

import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.PBKDF2;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

import redis.clients.jedis.Jedis;

/**
 * 
 * 通过旧密码修改登录密码第二步，输入新密码
 * 
 * @author j
 * 
 */
@Controller("/mobile/cop/2")
public class ChangingOldPwdStep2Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ChangingOldPwdStep2Api.class);

	@Autowired
	private IUserSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String step1key = req.getParameter("step1key");
		String passNew = req.getParameter("n");

		String userId = req.getUserId();

		User user = userDao.getUser(User.UserIDColumn, userId);

		if (null == user) {
			ret.put(status, C0006.toString());
			return ret;
		}
		if (StringUtils.isEmpty(passNew)) {
			return ret;
		}
//		if(!(Constants.P3.matcher(passNew).find()&&Constants.P2.matcher(passNew).find())&&!(!Constants.P3.matcher(passNew).find()&&Constants.P1.matcher(passNew).find())){
//			return ret;
//		}
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String succ = "cop:succ:" + user.getId();
			if (j.incrBy(succ, 0L) >= 2) {
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}
			// 验证第一步凭证
			if (!j.exists(step1key)) {
				ret.put(status, ErrorCode.C0040.toString());
				return ret;
			}
			if (PBKDF2.validate(passNew, user.getShadow())) {
				ret.put(status, ErrorCode.C0045.toString());
				return ret;
			}
			// 生成新密码
			String newShadow = PBKDF2.encode(passNew);
			userDao.userChangeProperty(userId, User.UserShadowColumn, newShadow);
			j.publish("PubModifiedPasswdUser", userId);

			ret.put(status, SUCCESS.toString());
			long succCount = j.incr(succ);
			if (succCount == 1)
				j.expire(succ, 24 * 60 * 60);
			
			
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			DataHelper.returnJedis(j);
		}
		return ret;
	}
}
