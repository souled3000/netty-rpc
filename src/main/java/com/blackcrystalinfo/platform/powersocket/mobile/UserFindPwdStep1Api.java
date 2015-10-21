package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0010;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.util.ErrorCode.C001D;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0027;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002D;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0033;
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

import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.DateUtils;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.VerifyCode;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;

@Controller("/step1")
public class UserFindPwdStep1Api extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdStep1Api.class);
	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());

		String destEmailAddr = req.getParameter("email");
		logger.info("UserFindPwdStep1Handler: email:{}", destEmailAddr);

		if (StringUtils.isBlank(destEmailAddr)) {
			r.put(status, C0010.toString());
			logger.info("email is null : email:{}|status:{}", destEmailAddr, r.get(status));
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

			// 用户是否存在
			User user = null;
			try {
				user = userDao.userGet(User.UserEmailColumn, destEmailAddr);
			} catch (Exception ex) {
				user = null;
			}

			if (null == user) {
				r.put(status, C0006.toString());
				return r;
			}

			// bug:未激活的邮箱不可以找回密码
			String emailAble = user.getEmailable();
			if (StringUtils.isBlank(emailAble)) {
				r.put(status, C0033.toString());
				return r;
			}

			// 修改密码次数
			int times = 0;
			String strTimes = j.get("user:passwdChangedTimes:" + user.getId());
			Long ttl = j.ttl("user:passwdChangedTimes:" + user.getId());
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

			// 发送找回密码邮件次数限制：一天内激活次数不超出5次
			times = 0;
			ttl = 0L;
			String findpwdtimes = j.get("user:findpwdtimes:" + user.getId());
			ttl = j.ttl("user:findpwdtimes:" + user.getId());
			if (StringUtils.isNotBlank(findpwdtimes)) {
				times = Integer.valueOf(findpwdtimes);
				if (times >= Constants.REGAGAIN_TIMES_MAX) {
					// 达到操作上限，不发送邮件
					logger.warn("Find password email had to the upper limit.");
					r.put("ttl", ttl);
					r.put(status, C002C.toString());
					return r;
				}
			}

			// 生成验证码
			String code = VerifyCode.randString(CODE_LENGTH);
			// 保存验证码
			j.del(destEmailAddr + "-code");
			j.del(destEmailAddr + "-code" + "fail");
			j.setex(destEmailAddr + "-code", CODE_EXPIRE, code);

			String subject = "验证码邮件";
			String content = "请尽快使用此验证码重置您的密码：" + code + " 验证超时时间：" + DateUtils.secToTime(CODE_EXPIRE);

			boolean b = SimpleMailSender.sendHtmlMail(destEmailAddr, subject, content);
			if (!b) {
				logger.info("sending Email failed!!!");
				r.put(status, C0011.toString());
				return r;
			}

			// 纪录激活次数
			times++;
			j.setex("user:findpwdtimes:" + user.getId(), Constants.REGAGAIN_EXPIRE, String.valueOf(times));

			r.put("findpwdtimes", times);
			if (times >= Constants.REGAGAIN_TIMES_NOTIC) {
				logger.info("Sending many times");
				r.put(status, C002D.toString());
			}
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			logger.error("UserFindPwdStep1", e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		logger.info("response: email:{}|status:{}", destEmailAddr, r.get(status));
		r.put(status, SUCCESS.toString());
		return r;
	}

}
