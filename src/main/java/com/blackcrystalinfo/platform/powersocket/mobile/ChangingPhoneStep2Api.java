package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C0042;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

import redis.clients.jedis.Jedis;

/**
 * 修改绑定手机第二步，校验旧手机发送的短信验证码
 * 
 * @author j
 * 
 */
@Controller("/mobile/cp/2")
public class ChangingPhoneStep2Api extends HandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(ChangingPhoneStep2Api.class);

	private static final int CODE_EXPIRE = Integer.valueOf(Constants.getProperty("validate.code.expire", "300"));


	@Autowired
	private IUserSvr usrSrv;

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, ErrorCode.SYSERROR);

		// 入参解析
		String step1key = req.getParameter("step1key");
		String code = req.getParameter("code");

		// phone是否格式正确？用户是否存在？
		String userId = req.getUserId();
		User user = usrSrv.getUser(User.UserIDColumn, userId);
		if (null == user) {
			ret.put(status, C0006.toString());
			return ret;
		}

		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			String succ = "cp:succ:"+user.getId();
			if(j.incrBy(succ,0L)>=2){
				ret.put(status, ErrorCode.C0046.toString());
				return ret;
			}
			// 获取第一步生成的code，未生成或已过期？
			String codeV = j.get(step1key);
			if (StringUtils.isBlank(codeV)) {
				ret.put(status, ErrorCode.C0040.toString());
				return ret;
			}

			// 用户输入的错误？
			if (!StringUtils.equals(code, codeV)) {
				ret.put(status, C0042.toString());
				return ret;
			}


			// 生成第二步凭证
			String step2keyV = UUID.randomUUID().toString();
			j.setex(step2keyV, CODE_EXPIRE, "");

			// 返回
			ret.put("step2key", step2keyV);
			ret.put(status, SUCCESS.toString());
		} catch (Exception e) {
			logger.info("occurn exception. ", e);
			return ret;
		} finally {
			DataHelper.returnJedis(j);
		}

		return ret;
	}
}
