package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0042;
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
import com.blackcrystalinfo.platform.common.LogType;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.powersocket.log.ILogger;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;

/**
 * 修改绑定手机第四步，验证新手机号码，旧手机解绑，新手机绑定
 * 
 * @author j
 * 
 */
@Controller("/mobile/cp/4")
public class ChangingPhoneStep4Api extends HandlerAdapter {
	private Logger logger = LoggerFactory.getLogger(ChangingPhoneStep4Api.class);

	@Autowired
	private IUserSvr userDao;
	@Autowired
	ILogger log;
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR.toString());

		// 入参解析
		String pz = req.getParameter("step3key");
		String code = req.getParameter("code");

		// phone是否格式正确？用户是否存在？
		String userId = req.getUserId();
		User user = userDao.getUser(User.UserIDColumn, userId);

		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String succ = "cp:succ:" + user.getId();
			if (j.incrBy(succ, 0L) >= 2) {
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}

			// 验证第三步凭证
			String v = j.get(pz);
			if (StringUtils.isBlank(v)) {
				ret.put(status, ErrorCode.C0040.toString());
				return ret;
			}
			String codeV = v.split("\\|")[0];
			String phone = v.split("\\|")[1];

			// 用户输入的错误？
			if (!StringUtils.equals(code, codeV)) {
				ret.put(status, C0042.toString());
				return ret;
			}

			// 输入无误,清除临时数据
			j.del(pz);

			// 数据入库
			userDao.updatePhone(userId, phone);
			ret.put(status, ErrorCode.SUCCESS.toString());
			long succCount = j.incr(succ);
			if (succCount == 1)
				j.expire(succ, 24 * 60 * 60);
			j.del(ChangingPhoneStep3Api.FREQ_KEY + user.getId());
			j.del(ChangingPhoneStep1Api.FREQ_KEY + user.getId());

			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			StringBuilder sms = new StringBuilder();
			sms.append(df.format(new Date())).append("您的").append(user.getUserName()).append("帐号成功绑定").append(phone).append("手机");
			logger.info("{}|{}|{}", userId, System.currentTimeMillis(),  sms.toString());
			SMSSender.send(phone, URLEncoder.encode(sms.toString(), "utf8"));
			log.write(String.format("%s|%s|%s|%s", userId,System.currentTimeMillis(),LogType.ZHAQ,sms.toString()));
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(j);
		}

		return ret;
	}
}
