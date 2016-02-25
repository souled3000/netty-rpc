package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.C0006;
import static com.blackcrystalinfo.platform.common.ErrorCode.C001E;
import static com.blackcrystalinfo.platform.common.ErrorCode.C001F;
import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.alibaba.fastjson.JSON;
import com.blackcrystalinfo.platform.common.BizCode;
import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.common.Utils;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;

import redis.clients.jedis.Jedis;

/**
 * 邀请家庭
 * 
 * @author Shenjz
 */
@Controller("/mobile/invitation")
public class InvitationApi extends HandlerAdapter {
//	private static final Logger logger = LoggerFactory.getLogger(InvitationApi.class);

	@Autowired
	IUserSvr userSvr;

	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> r = new HashMap<Object, Object>();
		r.put(status, SYSERROR.toString());
		String oper = req.getUserId();
		String uId = req.getParameter("uId");
		Jedis j = null;
		try {
			j = DataHelper.getJedis();

			User candidate = userSvr.getUser(User.UserIDColumn, uId);
			if (null == candidate) {
				r.put(status, C0006.toString());
				return r;
			}

			if(oper.equals(uId)){
				r.put(status, ErrorCode.C0047.toString());
				return r;
			}
			
			// 判断oper与uid是否为主
			// 如果oper是主或，oper与uid都不为主，可以进行邀请操作
			// 如果oper不是主，而uid是主，则不允许邀请操作

			String operFamily = j.hget("user:family", oper);
			String uFamily = j.hget("user:family", uId);

			// 操作员是别的家庭的成员，不具有添加成员的权限
			if (StringUtils.isNotBlank(operFamily) && !StringUtils.equals(operFamily, oper)) {
				r.put(status, C001E.toString());
				return r;
			}

			if (!StringUtils.isBlank(uFamily)) {
				r.put(status, C001F.toString());
				return r;
			}

			User operator = userSvr.getUser(User.UserIDColumn, oper);

			String hnick = operator.getNick();
			String nick = candidate.getNick();
			StringBuilder msg = new StringBuilder();
			Map<String, String> mm = new HashMap<String, String>();
			mm.put("hostId", oper);
			mm.put("hostNick", StringUtils.isBlank(hnick)?operator.getPhone():hnick);
			mm.put("mId", uId);
			mm.put("mNick", StringUtils.isBlank(nick)?candidate.getPhone():nick);
			msg.append(JSON.toJSON(mm));
			j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg(uId + "|", BizCode.FamilyInvite.getValue(), Integer.parseInt(uId), msg.toString()));

			// 用户确认加入家庭是有时效限制的
//			j.setex("user:invitationfamily:" + uId, Constants.USER_INVITATION_CFM_EXPIRE, oper);

			r.put(status, SUCCESS.toString());
		} catch (Exception e) {
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		return r;
	}
}
