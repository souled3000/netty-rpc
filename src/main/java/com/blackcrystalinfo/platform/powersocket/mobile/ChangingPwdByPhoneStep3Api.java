package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;

/**
 * 
 * 通过手机号码找回用户密码第三步，执行修改密码操作
 * 
 * @author j
 * 
 */
@Controller("/cpp/3")
public class ChangingPwdByPhoneStep3Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ChangingPwdByPhoneStep3Api.class);

	@Autowired
	private IUserSvr usrSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		// 解析参数
		String phone = req.getParameter("phone");
		String step2key = req.getParameter("step2key");
		String password = req.getParameter("w");

		// 校验参数
		if (StringUtils.isBlank(phone)) {
			return ret;
		}

		if (StringUtils.isBlank(step2key)) {
			return ret;
		}

		if (StringUtils.isBlank(password)) {
			return ret;
		}

		// 根据手机号获取用户信息
		User user =usrSvr.getUser(User.UserPhoneColumn, phone);
		if (null == user) {
			ret.put(status, C0006.toString());
			return ret;
		}
		
		String succCount = "B0037:"+user.getId()+":daily";

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			if (jedis.incrBy("B0037:" + user.getId() + ":daily", 0L) >= 2) {
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}
			// 验证第二步凭证
			String step2keyV = jedis.get(ChangingPwdByPhoneStep2Api.STEP2_KEY + user.getId());
			if (StringUtils.isBlank(step2keyV)) {
				ret.put(status, ErrorCode.C0040.toString());
				return ret;
			}
			if (!StringUtils.equals(step2keyV, step2key)) {
				return ret;
			}

			// 生成新密码
			String newShadow = PBKDF2.encode(password);
			usrSvr.userChangeProperty(user.getId(), User.UserShadowColumn, newShadow);
			jedis.publish("PubModifiedPasswdUser", user.getId());
			
			jedis.del("B0037:" + user.getId() + ":count");
			
			long dailyCount  = jedis.incr(succCount);
			if (dailyCount ==1 )
			jedis.expire(succCount,24*60*60);
			
			ret.put(status, SUCCESS.toString());
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SMSSender.send(phone, URLEncoder.encode(df.format(new Date())+"您的"+user.getUserName()+"帐号进行了密码重置操作","utf8"));
		} catch (Exception e) {
			logger.error("",e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
