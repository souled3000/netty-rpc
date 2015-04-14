package com.blackcrystalinfo.platform.powersocket.api;

import io.netty.handler.codec.http.HttpHeaders;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.util.DataHelper;
import com.blackcrystalinfo.platform.util.HttpUtil;
@Path(path="/sepia")
public class SepiaApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(SepiaApi.class);
	@Override
	public Object rpc(RpcRequest req) throws Exception {
		String cookie = req.getHeaders().get(HttpHeaders.Names.COOKIE);
		String sepia = HttpUtil.getPostValue(req.getParams(), "sepia");
		String code = HttpUtil.getPostValue(req.getParams(), "code");
		logger.info("sepia:{}|{}|{}",cookie,sepia,code);
		Jedis j = DataHelper.getJedis();
		Map ret = new HashMap();
		ret.put("status", "ffff");
		try{
			String word = j.get(cookie);
			if(word==null){
				ret.put("status", "001D");
				return ret;
			}
			Boolean isResponseCorrect = Boolean.FALSE;
//			isResponseCorrect = CaptchaServiceSingleton.getInstance().validateResponseForID(cookie, sepia.toUpperCase());
			isResponseCorrect = sepia.toUpperCase().equals(word);
			if (isResponseCorrect){
				j.setex(code+cookie, Captcha.expire, "succ");
				logger.info("sepia:{}|{}",word,sepia);
				ret.put("status", "0000");
			}else{
				ret.put("status", "0027");
				logger.info("sepia:{}|{}",word,sepia);
			}
		}catch(Exception e){
			DataHelper.returnBrokenJedis(j);
		}finally{
			DataHelper.returnJedis(j);
		}
		return ret;
	}
	
	public static void main(String[] args) throws Exception{
		String cookie = "65dedbe1-ea67-4fce-8394-f6d0309a43bb";
		String sepia = "7vsvdia";
		String code = "B0001";
		
		URL url = new URL("http://192.168.2.14:8181/sepia");
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Cookie", cookie);
		OutputStream raw  = con.getOutputStream();
		OutputStream buffered = new BufferedOutputStream(raw);
		OutputStreamWriter out = new OutputStreamWriter(buffered,"8859_1");
		out.write("sepia="+sepia+"&code="+code);
		out.flush();
		out.close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		System.out.println(reader.readLine());
		reader.close();
	}
}
