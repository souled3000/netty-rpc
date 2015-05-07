package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002B;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002D;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.mail.MailSenderInfo;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;

/**
 * 重新发送用户注册确认邮件
 * 
 * @author juliana
 *
 */
@Path(path = "/regagain")
public class RegAgainApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory
			.getLogger(RegAgainApi.class);

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String uuid = req.getParameter("uid");
		Jedis j = null;

		int times = 0;
		try {
			j = DataHelper.getJedis();

			// 判断邮件是否已经激活
			String actived = j.hget("user:emailavailable", uuid);
			if (null != actived && "true".equalsIgnoreCase(actived)) {
				logger.warn("email had been actived.");
				r.put(status, C002B.toString());
				return r;
			}

			// 已激活次数
			String activetimes = j.get("user:activetimes:" + uuid);
			if (null != activetimes && !"".equals(activetimes)) {
				times = Integer.valueOf(activetimes);
				if (times >= Constants.REGAGAIN_TIMES_MAX) {
					// 达到操作上限，不发送邮件
					logger.warn("Activing email had to the upper limit.");
					r.put(status, C002C.toString());
					return r;
				}
			}

			String email = j.hget("user:email", uuid);

			String emailAddr = Constants.getProperty("email.user", "");
			String emailPwd = Constants.getProperty("email.pwd", "");
			String mailHost = Constants.getProperty("mail.server.host", "");
			String mailPost = Constants.getProperty("mail.server.port", "");

			String protocol = Constants.SERVERPROTOCOL;
			String ip = Constants.SERVERIP;
			String port = Constants.SERVERPORT;

			MailSenderInfo mailInfo = new MailSenderInfo();
			mailInfo.setMailServerHost(mailHost);
			mailInfo.setMailServerPort(mailPost);
			mailInfo.setValidate(true);
			mailInfo.setUserName(emailAddr);
			mailInfo.setPassword(emailPwd);// 您的邮箱密码
			mailInfo.setFromAddress(emailAddr);
			mailInfo.setToAddress(email);
			mailInfo.setSubject("用户注册确认");

			String linkAddr = protocol + "://" + ip + ":" + port + "/cfm?v="
					+ uuid;
			StringBuilder sb = new StringBuilder();
			sb.append("点击如下链接马上完成邮箱验证：");
			sb.append("<br>");
			sb.append("<a href='" + linkAddr + "'>激活</a>");
			sb.append("<br>");
			sb.append("<br>");
			sb.append("如果链接无法点击，请完整拷贝到浏览器地址栏里直接访问，链接如下：");
			sb.append("<br>");
			sb.append(linkAddr);

			mailInfo.setContent(sb.toString());
			boolean b = SimpleMailSender.sendHtmlMail(mailInfo);
			if (!b) {
				logger.info("sending Email failed!!!");
				r.put(status, C0011.toString());
				return r;
			}

			// 纪录激活次数
			times++;
			j.setex("user:activetimes:" + uuid, Constants.REGAGAIN_EXPIRE,
					String.valueOf(times));
			r.put("activetimes", times);
			if (times >= Constants.REGAGAIN_TIMES_NOTIC) {
				logger.info("Sending many times");
				r.put(status, C002D.toString());
				return r;
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, ErrorCode.SUCCESS.toString());
		return r;
	}
}
