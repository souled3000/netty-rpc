package com.blackcrystalinfo.platform.util;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.ArrayUtils;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

import redis.clients.jedis.Jedis;

public class Utils {
	
	public static byte[] genMsg(String head,int bizCode, long id, String msg){
		return ArrayUtils.addAll(head.getBytes(), genDatagram4Long(bizCode,id,msg));
	}
	public static byte[] genMsg(String head,int bizCode, String mac, String msg){
		return ArrayUtils.addAll(head.getBytes(), genDatagram4Mac(bizCode,mac,msg));
	}
	
	public static byte[] genDatagram4Long(int bizCode, long id, String msg) {

		try {
			byte[] m;
			m = msg.getBytes("utf8");
			int ml = m.length;
			byte[] r = new byte[10 + ml];
			r[9] = (byte) ml;
			byte[] bId = NumberByte.long2Byte(id);
			r[0] = (byte) bizCode;
			for (int i = 0; i < bId.length; i++) {
				r[1 + i] = bId[i];
			}
			for (int i = 0; i < m.length; i++) {
				r[10 + i] = m[i];
			}
			return r;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static byte[] genDatagram4Mac(int bizCode, String mac, String msg) {
		
		try {
			byte[] m;
			m = msg.getBytes("utf8");
			int ml = m.length;
			byte[] r = new byte[10 + ml];
			r[9] = (byte) ml;
			byte[] bId = ByteUtil.fromHex(mac);
			r[0] = (byte) bizCode;
			for (int i = 0; i < bId.length; i++) {
				r[1 + i] = bId[i];
			}
			for (int i = 0; i < m.length; i++) {
				r[10 + i] = m[i];
			}
			return r;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void printDatagram4Websocket(int[] r) {
		
		int biz = (int)r[0];
		System.out.println("biz:"+biz);
		
		byte[] bId = new byte[8];
		for(int i = 1;i<9;i++){
			bId[i-1]=(byte)r[i];
		}
		
		long id = NumberByte.byte2Long(bId);
		System.out.println("id:"+id);
		System.out.println("ml:"+r[9]);
		int ml = (int)r[9];
		byte[] bmsg = new byte[ml];
		
		System.out.println("ml:"+ml);
		for(int i=0;i<ml;i++){
			bmsg[i] = (byte)r[10+i];
		}
		try {
			String msg = new String(bmsg,"utf8");
			System.out.println(msg);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
//		byte[] a = genDatagram4Long(1,-30,"哈哈");
//		int[] a = {5,0,0,0,0,0,0,8,224,85,123,34,104,111,115,116,73,100,34,58,34,49,55,52,52,34,44,34,104,111,115,116,78,105,99,107,34,58,34,229,176,143,233,186,166,229,146,140,229,176,154,233,131,189,228,188,154,34,44,34,109,73,100,34,58,34,50,50,55,50,34,44,34,109,78,105,99,107,34,58,34,233,187,145,232,137,178,230,176,180,230,153,182,34,125};
//		int[] a = {1,0,0,0,0,0,0,11,160,82,123,34,104,111,115,116,73,100,34,58,34,49,55,52,52,34,44,34,104,111,115,116,78,105,99,107,34,58,34,229,176,143,233,186,166,229,146,140,229,176,154,233,131,189,228,188,154,34,44,34,109,73,100,34,58,34,50,57,55,54,34,44,34,109,78,105,99,107,34,58,34,230,150,176,229,138,160,229,157,161,34,125};
//		int[] a = {9,249,255,255,255,255,255,255,255,0};
//		int[] a = {5,0,0,0,0,0,0,4,128,75,123,34,104,111,115,116,73,100,34,58,34,49,55,52,52,34,44,34,104,111,115,116,78,105,99,107,34,58,34,229,176,143,233,186,166,229,146,140,229,176,154,233,131,189,228,188,154,34,44,34,109,73,100,34,58,34,49,49,53,50,34,44,34,109,78,105,99,107,34,58,34,120,108,34,125};
		int[] a = {1,0,0,0,0,0,0,4,128,75,123,34,104,111,115,116,73,100,34,58,34,49,55,52,52,34,44,34,104,111,115,116,78,105,99,107,34,58,34,229,176,143,233,186,166,229,146,140,229,176,154,233,131,189,228,188,154,34,44,34,109,73,100,34,58,34,49,49,53,50,34,44,34,109,78,105,99,107,34,58,34,120,108,34,125};
		
		System.out.println("--------------------------");
		
		printDatagram4Websocket(a);
	}
	
	public static void main2(String[] args) throws Exception{
		Jedis j = DataHelper.getJedis();
		j.publish("PubCommonMsg:0x36".getBytes(), Utils.genMsg("-32,-48|",5, Integer.parseInt("48"), "黄河"));
		DataHelper.returnJedis(j);
	}
}
