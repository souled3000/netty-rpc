package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.CookieUtil;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;

@Controller("/mobile/mailchange")
public class MailChangeApi extends HandlerAdapter {
	private Logger logger = LoggerFactory.getLogger(MailChangeApi.class);

	@Autowired
	private ILoginSvr userDao;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR);

		// 入参解析：email， phone
		String cookie = req.getParameter("cookie");
		String email = req.getParameter("email");

		// 用户是否存在？
		String userId = CookieUtil.gotUserIdFromCookie(cookie);
		User user = null;
		try {
			user = userDao.userGet(User.UserIDColumn, userId);
			if (null == user) {
				throw new Exception("user is null");
			}
		} catch (Exception e) {
			logger.error("cannot find user by id.", e);
			ret.put(status, "用户没找到");
			return ret;
		}

		// 用户已经绑定手机号码？
		String emailOld = user.getEmail();
		if (email.equalsIgnoreCase(emailOld)) {
			ret.put(status, "已绑定邮箱，请换一个");
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			String subject = "修改邮箱激活邮件";
			String protocol = Constants.SERVERPROTOCOL;
			String ip = Constants.SERVERIP;
			String port = Constants.SERVERPORT;

			String uuid = UUID.randomUUID().toString();
			String linkAddr = protocol + "://" + ip + ":" + port + "/cfm?v=" + uuid;

			StringBuilder sb = new StringBuilder();
			sb.append("点击如下链接马上完成邮箱验证：");
			sb.append("<br>");
			sb.append("<a href='" + linkAddr + "'>激活</a>");
			sb.append("<br>");
			sb.append("<br>");
			sb.append("如果链接无法点击，请完整拷贝到浏览器地址栏里直接访问，链接如下：");
			sb.append("<br>");
			sb.append(linkAddr);

			boolean b = SimpleMailSender.sendHtmlMail(email, subject, sb.toString());
			if (!b) {
				logger.info("sending Email failed!!!");
				ret.put(status, C0011.toString());
				return ret;
			}

			// 连接有效期
			jedis.setex("user:mailActiveUUID:" + userId, Constants.MAIL_ACTIVE_EXPIRE, uuid);
			jedis.setex("user:mailActive:" + uuid, Constants.MAIL_ACTIVE_EXPIRE, userId);

			// 数据入库
			userDao.userChangeProperty(userId, User.UserEmailColumn, email);
			userDao.userChangeProperty(userId, User.UserPhoneableColumn, "false");

			// 返回
			ret.put(status, ErrorCode.SUCCESS);
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}
}
