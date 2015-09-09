package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.RespField.status;
import io.netty.handler.codec.http.multipart.MixedFileUpload;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.powersocket.data.User;
import com.blackcrystalinfo.platform.service.ILoginSvr;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;

@Controller("/mobile/faceup")
public class FaceUpApi extends HandlerAdapter {

	@Autowired
	private ILoginSvr userDao;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object rpc(RpcRequest req) throws Exception {
		Map r = new HashMap();
		String cookie = req.getParameter("cookie");
		String nick = req.getParameter("nick");
		String id = CookieUtil.gotUserIdFromCookie(cookie);
		MixedFileUpload pic = (MixedFileUpload) req.getParams().getBodyHttpData("pic");
		if (pic != null && pic.get() != null) {
			File f = new File(Constants.PIC_PATH + File.separator + id);
			FileUtils.copyFile(pic.getFile(), f);
			pic.getFile().delete();
		}

		if (StringUtils.isNotBlank(nick)) {
			try {
				userDao.userChangeProperty(id, User.UserNickColumn, nick);
			} catch (Exception e) {
				// DataHelper.returnBrokenJedis(j);
			}
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
}
