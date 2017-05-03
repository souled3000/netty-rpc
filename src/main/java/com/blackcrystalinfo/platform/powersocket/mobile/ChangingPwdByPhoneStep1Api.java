package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0037;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0038;
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
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;

/**
 * 
 * 通过手机号码找回用户密码第一步，重发找回密码也调用此接口。
 * 
 * @author j
 * 
 */
@Controller("/cpp/1")
public class ChangingPwdByPhoneStep1Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ChangingPwdByPhoneStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Autowired
	private IUserSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");

		if (StringUtils.isBlank(phone)) {
			return ret;
		}

		User user = userDao.getUser(User.UserPhoneColumn, phone);
		if (null == user) {
			ret.put(status, C0006.toString());
			return ret;
		}
		
		String userId = user.getId();
		
		Jedis j = null;

		try {
			j = JedisHelper.getJedis();
			
			if(j.incrBy("B0037:succ:"+user.getId(),0L)>=2){
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}
			
			String invokeCount = "B0037:count:" + userId;
			long count = j.incrBy(invokeCount,0);
			if (count >= Constants.USER_COMMON_TIMES) {
				ret.put(status, C002C.toString());
				return ret;
			}
			String freq = "B0037:30s:"+user.getId();
			if (j.exists(freq)) {
				ret.put(status, C0037.toString());
				return ret;
			}
			j.setex(freq,30,"1");
			
			// send message
			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put(status, C0038.toString());
				return ret;
			}
			
			count = j.incr(invokeCount);
			if(count==1)
			j.expire(invokeCount, 24*60*60);
			
			ret.put("count", count);
			// 生成第一步凭证
			String step1key = UUID.randomUUID().toString();
			j.setex(step1key+phone, CODE_EXPIRE, code);
			ret.put("step1key", step1key);
			ret.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			JedisHelper.returnJedis(j);
		}

		return ret;

	}
}
