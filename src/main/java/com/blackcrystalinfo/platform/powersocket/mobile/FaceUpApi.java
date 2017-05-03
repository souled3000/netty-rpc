package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.io.File;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.JedisHelper;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

import io.netty.handler.codec.http.multipart.MixedFileUpload;
import redis.clients.jedis.Jedis;

@Controller("/mobile/faceup")
public class FaceUpApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(FaceUpApi.class);
	private static MessageDigest md5;

	static {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {

		} finally {

		}
	}

	@Autowired
	private IUserSvr userDao;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object rpc(RpcRequest req) throws Exception {
		Map r = new HashMap();
		String nick = req.getParameter("nick");
		String id = req.getUserId();
		String facestamp = null;

		MixedFileUpload pic = (MixedFileUpload) req.getParams().getBodyHttpData("pic");
		if (pic != null && pic.get() != null) {
			File f = new File(Constants.PIC_PATH + File.separator + id);
			FileUtils.copyFile(pic.getFile(), f);

			// 计算上传头像的MD5值
			facestamp = ByteUtil.toHex(md5.digest(pic.get()));
			pic.getFile().delete();
		}

		if (StringUtils.isNotBlank(nick)) {
			userDao.userChangeProperty(id, User.UserNickColumn, nick);
		}

		// 保存上传头像的MD5值
		Jedis jedis = null;
		try {
			jedis = JedisHelper.getJedis();
			jedis.hset("user:facestamp", id, facestamp);
		} catch (Exception e) {
			logger.error("face up set stamp error!!!", e);
		} finally {
			JedisHelper.returnJedis(jedis);
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
