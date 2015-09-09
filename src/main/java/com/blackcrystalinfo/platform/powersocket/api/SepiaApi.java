package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.C001D;
import static com.blackcrystalinfo.platform.util.ErrorCode.C0027;
import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
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
import org.springframework.stereotype.Controller;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.captcha.Captcha;
import com.blackcrystalinfo.platform.util.DataHelper;

@Controller("/sepia")
public class SepiaApi extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(SepiaApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		String cookie = req.getHeaders().get(HttpHeaders.Names.COOKIE);
		String sepia = req.getParameter("sepia");
		String code = req.getParameter("code");
		logger.info("sepia:{}|{}|{}", cookie, sepia, code);
		Jedis j = DataHelper.getJedis();
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("status", SYSERROR.toString());
		try {
			String word = j.get(cookie);
			if (word == null) {
				ret.put("status", C001D.toString());
				return ret;
			}
			Boolean isResponseCorrect = Boolean.FALSE;
			isResponseCorrect = sepia.toUpperCase().equals(word);
			if (isResponseCorrect) {
				j.setex(code + cookie, Captcha.expire, "succ");
				logger.info("sepia:{}|{}", word, sepia);
				ret.put("status", SUCCESS.toString());
			} else {
				j.setex(code + cookie, Captcha.expire, "failed");
				ret.put("status", C0027.toString());
				logger.info("sepia:{}|{}", word, sepia);
			}
		} catch (Exception e) {
			logger.error("exception occurn! e=", e);
		} finally {
			DataHelper.returnJedis(j);
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		String cookie = "65dedbe1-ea67-4fce-8394-f6d0309a43bb";
		String sepia = "7vsvdia";
		String code = "B0001";

		URL url = new URL("http://192.168.2.14:8181/sepia");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Cookie", cookie);
		OutputStream raw = con.getOutputStream();
		OutputStream buffered = new BufferedOutputStream(raw);
		OutputStreamWriter out = new OutputStreamWriter(buffered, "8859_1");
		out.write("sepia=" + sepia + "&code=" + code);
		out.flush();
		out.close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		System.out.println(reader.readLine());
		reader.close();
	}
}
