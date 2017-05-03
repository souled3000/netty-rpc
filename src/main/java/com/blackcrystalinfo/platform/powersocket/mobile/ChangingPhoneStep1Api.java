package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0037;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0038;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
 * 修改绑定手机号码的第一步，发送短信验证码。
 * 
 * @author j
 * 
 */
@Controller("/mobile/cp/1")
public class ChangingPhoneStep1Api extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(ChangingPhoneStep1Api.class);

	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	public static final String FREQ_KEY = "B0032:count:";

	@Autowired
	private IUserSvr usrSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String userId = req.getUserId();
		User user = usrSvr.getUser(User.UserIDColumn, userId);

		String oldPhone = user.getPhone();

		Jedis j = null;
		try {
			j = JedisHelper.getJedis();
			String succ = "cp:succ:"+user.getId();
			if(j.incrBy(succ,0L)>=2){
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}

			long times = j.incrBy(FREQ_KEY + userId,0);
			if (times >= Constants.USER_COMMON_TIMES) {
				ret.put(status, C002C.toString());
				return ret;
			}
			
			String codeExpr = "B0032:30s:"+user.getId();
			if (j.exists(codeExpr)) {
				ret.put(status, C0037.toString());
				return ret;
			}
			j.setex(codeExpr,30,"1");
			

			// 生成验证码，服务器端临时存储
			String code = VerifyCode.randString(CODE_LENGTH);

			// 发送验证码是否成功？
			if (!SMSSender.send(oldPhone, code)) {
				ret.put(status, C0038.toString());
				return ret;
			}

			// 更新状态记录
			times = j.incr(FREQ_KEY + userId);
			if(times==1)
				j.expire(FREQ_KEY + userId, 24*60*60);

			// 生成第一步凭证
			String pz = UUID.randomUUID().toString();
			j.setex(pz, CODE_EXPIRE, code);

			ret.put("count", times);
			ret.put("step1key", pz);
			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			JedisHelper.returnJedis(j);
		}

		return ret;
	}
}
