package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C0011;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
 * @author juliana
 *
 */
@Path(path="/regagain")
public class RegAgainApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(RegAgainApi.class);

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String uuid = req.getParameter( "uid");
		Jedis j = null;
		try {
			j=DataHelper.getJedis();
			String email = j.get(uuid+"mail");
			if(StringUtils.isBlank(email)){
				r.put(status, ErrorCode.C0028);
				return r;
			}
			j.setex(uuid+"mail", Constants.USRCFMEXPIRE, email);

			j.setex(uuid+"phone", Constants.USRCFMEXPIRE, j.get(uuid+"phone"));

			j.setex(uuid+"shadow", Constants.USRCFMEXPIRE, j.get(uuid+"shadow"));
			
			j.setex(uuid+"nick", Constants.USRCFMEXPIRE, j.get(uuid+"shadow"));
			
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
			mailInfo.setContent("<a href='"+protocol+ "//"+ip+":"+port+"/cfm?v=" + uuid+"'>激活</a>");
			boolean b = SimpleMailSender.sendHtmlMail(mailInfo);
			if(!b){
				logger.info("sending Email failed!!!");
				r.put(status, C0011.toString());
				return r;
			}
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}
		r.put(status, ErrorCode.SUCCESS);
		return r;
	}
}
