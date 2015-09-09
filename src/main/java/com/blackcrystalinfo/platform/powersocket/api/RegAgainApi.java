package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002B;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002C;
import static com.blackcrystalinfo.platform.util.ErrorCode.C002D;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.ErrorCode;
import com.blackcrystalinfo.platform.util.mail.SimpleMailSender;

/**
 * 重新发送用户注册确认邮件
 * 
 * @author juliana
 * 
 */
@Controller("/regagain")
public class RegAgainApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(RegAgainApi.class);

	@Autowired
	private ILoginSvr userSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String uid = req.getParameter("uid");
		Jedis j = null;

		int times = 0;
		try {
			j = DataHelper.getJedis();

			User user = userSvr.userGet(User.UserIDColumn, uid);

			// 判断邮件是否已经激活
			String actived = user.getEmailable();
			if (null != actived && "true".equalsIgnoreCase(actived)) {
				logger.warn("email had been actived.");
				r.put(status, C002B.toString());
				return r;
			}

			// 已激活次数
			String activetimes = j.get("user:activetimes:" + uid);
			Long ttl = j.ttl("user:activetimes:" + uid);
			if (null != activetimes && !"".equals(activetimes)) {
				times = Integer.valueOf(activetimes);
				if (times >= Constants.REGAGAIN_TIMES_MAX) {
					// 达到操作上限，不发送邮件
					logger.warn("Activing email had to the upper limit.");
					r.put("ttl", ttl);
					r.put(status, C002C.toString());
					return r;
				}
			}

			String email = user.getEmail();
			String subject = "用户注册确认";

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
				r.put(status, C0011.toString());
				return r;
			}

			// 纪录激活次数
			times++;
			j.setex("user:activetimes:" + uid, Constants.REGAGAIN_EXPIRE, String.valueOf(times));
			// 连接有效期
			String oldUUID = j.get("user:mailActiveUUID:" + uid);
			j.del("user:mailActive:" + oldUUID); // 删除旧的激活连接

			j.setex("user:mailActiveUUID:" + uid, Constants.MAIL_ACTIVE_EXPIRE, uuid);
			j.setex("user:mailActive:" + uuid, Constants.MAIL_ACTIVE_EXPIRE, uid);
			r.put("activetimes", times);
			if (times >= Constants.REGAGAIN_TIMES_NOTIC) {
				logger.info("Sending many times");
				r.put(status, C002D.toString());
				return r;
			}
		} catch (Exception e) {
			// DataHelper.returnBrokenJedis(j);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, ErrorCode.SUCCESS.toString());
		return r;
	}

}
