package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0010;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.util.ErrorCode.C001D;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.DateUtils;
import com.blackcrystalinfo.platform.util.VerifyCode;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

/**
 * 
 * @author j
 * 
 */
@Controller("/byphonestep1")
public class UserFindPwdByPhoneStep1Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdByPhoneStep1Api.class);
	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");
		logger.info("find pwd by phone, phone:{}", phone);

		if (StringUtils.isBlank(phone)) {
			r.put(status, C0010.toString());
			logger.info("phone is blank, phone:{}|status:{}", phone, r.get(status));
			return r;
		}

		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			// 图片验证码
			String cookie = req.getHeaders().get(HttpHeaders.Names.COOKIE);
			String flag = j.get("B0012" + cookie);
			if (Captcha.validity) {
				if (flag == null) {
					r.put(status, C001D.toString());
					logger.debug("captcha fail.");
					return r;
				} else if (!flag.equals("succ")) {
					r.put(status, C0027.toString());
					logger.debug("captcha fail.");
					return r;
				}
			}

			// 手机号码是否存在
			User user = null;
			try {
				user = userDao.userGet(User.UserPhoneColumn, phone);
			} catch (Exception ex) {
				user = null;
			}

			if (null == user) {
				r.put(status, C0006.toString());
				return r;
			}

			// 生成验证码
			String code = VerifyCode.randString(CODE_LENGTH);
			// 保存验证码
			j.del(phone + "-code");
			j.del(phone + "-code" + "fail");
			j.setex(phone + "-code", CODE_EXPIRE, code);

			String content = "请尽快使用此验证码重置您的密码：" + code + " 验证超时时间：" + DateUtils.secToTime(CODE_EXPIRE);

			boolean b = SMSSender.send(phone, content);
			if (!b) {
				logger.info("sending sms failed!!!");
				r.put(status, C0011.toString());
				return r;
			}

		} catch (Exception e) {
			logger.error("find pwd by phone exception!!!", e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		logger.info("response: phone:{}|status:{}", phone, r.get(status));
		r.put(status, SUCCESS.toString());
		return r;
	}

}
