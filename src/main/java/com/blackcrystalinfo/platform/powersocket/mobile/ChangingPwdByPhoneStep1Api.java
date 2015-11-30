package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0035;
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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

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
			ret.put(status, C0035.toString());
			return ret;
		}

		User user = userDao.getUser(User.UserPhoneColumn, phone);
		if (null == user) {
			ret.put(status, C0006.toString());
			return ret;
		}
		
		String userId = user.getId();
		
		Jedis jedis = null;

		try {
			jedis = DataHelper.getJedis();
			
			if(jedis.incrBy("B0037:"+user.getId()+":daily",0L)>=2){
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}
			
			String codeExpr = "B0037:"+user.getId()+":30s";
			if (jedis.exists(codeExpr)) {
				ret.put(status, C0037.toString());
				return ret;
			}
			String invokeCount = "B0037:" + userId + ":count";
			long count = jedis.incr(invokeCount);
			if (count >= Constants.DAILYTHRESHOLD) {
				ret.put(status, C002C.toString());
				return ret;
			}
			// send message
			String code = VerifyCode.randString(CODE_LENGTH);
			if (!SMSSender.send(phone, code)) {
				ret.put(status, C0038.toString());
				return ret;
			}
			
			if(count==1)
			jedis.expire(invokeCount, 24*60*60);
			
			ret.put("count", count);
			// 生成第一步凭证
			String step1key = UUID.randomUUID().toString();
			jedis.setex(step1key, CODE_EXPIRE, code);
			jedis.setex(codeExpr,30,"");
			ret.put("step1key", step1key);
			ret.put(status, ErrorCode.SUCCESS.toString());
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;

	}
}
