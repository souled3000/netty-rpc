package com.blackcrystalinfo.platform.powersocket.handler;

import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.util.cryto.SourceKey;

public class SessHandler extends HandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(SessHandler.class);

	@Override
	public Object rpc(JSONObject req) throws InternalException {
		logger.info("SessHandler start");
		SessResponse resp = new SessResponse();
		resp.setStatus(-1);
		String idn = req.getString("idn");
		if (!validateIdn(idn)) {
			return null;
		}

		try {
			String key = SourceKey.gen();
			resp.setKey(key);
			resp.setStatus(0);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resp;
	}

	private class SessResponse {
		private String key;
		private int status;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

	}

	private boolean validateIdn(String idn) {
		return true;
	}

	public static void main(String[] args) {
		String s = "{idn:'0123456789'}";

		JSONObject obj = JSONObject.parseObject(s);
		System.out.println(obj.get("idn"));
		System.out.println(String.format("%s", "lchj"));

	}
}
