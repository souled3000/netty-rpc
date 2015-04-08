package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;
import io.netty.handler.codec.http.multipart.MixedFileUpload;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.HttpUtil;
@Path(path="/mobile/faceup")
public class FaceUpApi extends HandlerAdapter {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object rpc(RpcRequest req) throws Exception {
		Map r = new HashMap();
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String nick = HttpUtil.getPostValue(req.getParams(), "nick");
		String id = CookieUtil.gotUserIdFromCookie(cookie);
		MixedFileUpload pic  = (MixedFileUpload)req.getParams().getBodyHttpData("pic");
		if (pic != null && pic.get() != null) {
			File f = new File(Constants.PIC_PATH + File.separator + id);
			FileUtils.copyFile(pic.getFile(), f);
			pic.getFile().delete();
			r.put(status, SUCCESS.toString());
			return r;
		}
		Jedis j=null;
		try{
			j=DataHelper.getJedis();
			j.hset("user:nick", id, nick);
		}catch(Exception e){
			DataHelper.returnBrokenJedis(j);
		}finally{
			DataHelper.returnJedis(j);
		}
		r.put(status, SYSERROR.toString());
		return r;
	}
}
