package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0036;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
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
 * 手机号码注册第三步：入库
 * 
 * @author j
 * 
 */
@Controller("/rp/3")
public class RegisterStep3Api extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(RegisterStep3Api.class);

	@Autowired
	IUserSvr usrSvr;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		String phone = req.getParameter("phone");
		String step2key = req.getParameter("step2key");
		String pwd = req.getParameter("w");
		if (StringUtils.isBlank(phone)) {
			return ret;
		}
		if (StringUtils.isBlank(step2key)) {
			return ret;
		}
		if(!(Constants.P3.matcher(pwd).find()&&Constants.P2.matcher(pwd).find())&&!(!Constants.P3.matcher(pwd).find()&&Constants.P1.matcher(pwd).find())){
			return ret;
		}
		if (usrSvr.userExist(phone)) {
			ret.put(status, C0036.toString());
			return ret;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();

			
			// 验证第二步凭证
			if (!jedis.exists(step2key)) {
				ret.put(status, ErrorCode.C0040.toString());
				return ret;
			}

			// 手机号是否已经注册
			boolean exist = usrSvr.userExist(phone);
			if (exist) {
				ret.put(status, ErrorCode.C0036.toString());
				return ret;
			}

			// 注册用户信息
			usrSvr.saveUser(phone, phone, PBKDF2.encode(pwd));
			String userId = usrSvr.getUser(User.UserNameColumn, phone).getId();

			ret.put("uId", userId);
			ret.put(status, SUCCESS.toString());
			String sms = new String("注册成功");
			SMSSender.send(phone, URLEncoder.encode(sms,"utf8"));
			logger.info("{}|{}|{}",userId,System.currentTimeMillis(),sms);
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			DataHelper.returnJedis(jedis);
		}

		return ret;
	}

	public static void main(String[] args) {
		String pwd = "111111333";
		if(Constants.P3.matcher(pwd).find()&&Constants.P2.matcher(pwd).find()){
			System.out.println("a合格");
			
		}else if(!Constants.P3.matcher(pwd).find()&&Constants.P1.matcher(pwd).find()){
			System.out.println("b合格");
			
		}else{
			System.out.println("a不合格");
		}
		if(!(Constants.P3.matcher(pwd).find()&&Constants.P2.matcher(pwd).find())&&!(!Constants.P3.matcher(pwd).find()&&Constants.P1.matcher(pwd).find())){
			System.out.println("b不合格");
		}
		System.out.println(StringUtils.isEmpty("    "));
		System.out.println(StringUtils.isBlank("    "));
	}
}
