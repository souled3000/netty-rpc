package com.blackcrystalinfo.platform.util.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class SimpleHttpClientUtil {
	public static String beg(String url, String queryParam) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			// 建立连接
			URL hostURL = new URL(url + "?" + queryParam);
			URLConnection conn = hostURL.openConnection();

			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} finally {
			if (null != br) {
				br.close();
			}
		}

		return sb.toString();
	}

	public static void main(String[] args) {
		String ret;
		try {
			ret = SimpleHttpClientUtil.beg("http://localhost/sendSms.php", "type=submit&phone=18612455087&content=123123123");
			System.out.println(ret);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
