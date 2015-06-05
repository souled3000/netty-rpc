package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.RespField.status;
import io.netty.handler.codec.http.multipart.MixedFileUpload;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.Constants;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;

@Path(path="/mobile/faceup")
public class FaceUpApi extends HandlerAdapter {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object rpc(RpcRequest req) throws Exception {
		Map r = new HashMap();
		String cookie = req.getParameter( "cookie");
		String nick = req.getParameter( "nick");
		String id = CookieUtil.gotUserIdFromCookie(cookie);
		MixedFileUpload pic  = (MixedFileUpload)req.getParams().getBodyHttpData("pic");
		if (pic != null && pic.get() != null) {
			File f = new File(Constants.PIC_PATH + File.separator + id);
			FileUtils.copyFile(pic.getFile(), f);
			pic.getFile().delete();
		}

		Jedis j = null;
		if (StringUtils.isNotBlank(nick)) {
			try {
				j = DataHelper.getJedis();
				j.hset("user:nick", id, nick);
			} catch (Exception e) {
				//DataHelper.returnBrokenJedis(j);
			} finally {
				DataHelper.returnJedis(j);
			}
		}

		r.put(status, SUCCESS.toString());
		return r;
	}
	
	public static void main(String[] args) throws Exception {

//		URI uri = URI.create("http://192.168.2.14:8181/mobile/faceup");
//		InetSocketAddress sa = new InetSocketAddress("192.168.2.14", 8181);
//		Proxy proxy = new Proxy(Type.HTTP, sa);
		File file = new File("d:\\16");
//		URLConnection conn = uri.toURL().openConnection(proxy);
		
		URL url = new URL("http://192.168.2.14:8181/mobile/faceup");
		URLConnection conn = url.openConnection();
		
		String boundary = "---------------------------" + System.currentTimeMillis();
		String boundaryInContent = "--" + boundary;
		String rn = "\r\n";
		conn.addRequestProperty("Connection", "keep-alive");
		conn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setConnectTimeout(1000);
		conn.setUseCaches(false);
		conn.connect();
		OutputStream out = conn.getOutputStream();
		StringBuilder sb = new StringBuilder();
		
		sb.append(boundaryInContent).append(rn);
		sb.append("Content-Disposition: form-data; name=nick").append(rn).append(rn);
		sb.append("杨乐").append(rn);
		sb.append(boundaryInContent).append(rn);
		sb.append("Content-Disposition: form-data; name=cookie").append(rn).append(rn);
		sb.append("MTZ8MzAwfDFkM2U0ZDUyYWMxOTQ1MjQ4ODQ1OTM4ZTc3NTgyMTE2ODUwOTlk-F412AB9F29A98DC242B1EAA44B4F936B").append(rn);
		sb.append(boundaryInContent).append(rn);
		sb.append("Content-Disposition: form-data; name=pic; filename=" + file.getName()).append(rn);
		sb.append("Content-Type: application/octet-stream").append(rn).append(rn);
		out.write(sb.toString().getBytes());
		out.write(FileUtils.readFileToByteArray(file));
		sb.delete(0, sb.length());
		sb.append(rn).append(boundaryInContent).append("--").append(rn).append(rn);
		out.write(sb.toString().getBytes());
		
		out.flush();
		out.close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String res = reader.readLine();
		reader.close();
		System.out.println("return: " + res);

	}

}
