package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0010;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.common.ErrorCode.C002D;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0033;
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

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.common.DateUtils;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.VerifyCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;

@Controller("/step1again")
public class UserFindPwdStep1AgainApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(UserFindPwdStep1AgainApi.class);
	private static final int CODE_LENGTH = Integer.valueOf(Constants.getProperty("validate.code.length", "6"));
	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));

	@Autowired
	private IUserSvr userDao;

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
			j = JedisHelper.getJedis();

			// 查找email
			if (!userDao.userExist(destEmailAddr)) {
				r.put(status, C0006.toString());
				return r;
			}

			User user = userDao.getUser(User.UserNameColumn, destEmailAddr);
			String userId = user.getId();

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

			// 重新发送找回密码邮件次数
			times = 0;
			ttl = 0L;
			String findpwdtimes = j.get("user:findpwdtimes:" + userId);
			ttl = j.ttl("user:findpwdtimes:" + user.getId());
			if (null != findpwdtimes && !"".equals(findpwdtimes)) {
				times = Integer.valueOf(findpwdtimes);
				if (times >= Constants.DAILYTHRESHOLD) {
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
			j.setex("user:findpwdtimes:" + userId, Constants.REGAGAIN_EXPIRE, String.valueOf(times));
			r.put("findpwdtimes", times);
			if (times >= Constants.REGAGAIN_TIMES_NOTIC) {
				logger.info("Sending many times");
				r.put(status, C002D.toString());
				return r;
			}
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			logger.error("UserFindPwdStep1", e);
			return r;
		} finally {
			JedisHelper.returnJedis(j);
		}
		logger.info("response: email:{}|status:{}", destEmailAddr, r.get(status));
		r.put(status, SUCCESS.toString());
		return r;
	}

}