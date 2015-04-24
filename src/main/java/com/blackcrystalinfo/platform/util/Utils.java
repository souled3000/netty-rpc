package com.blackcrystalinfo.platform.util;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.ArrayUtils;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

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

	public static void printDatagram4Websocket(byte[] r) {
		
		int biz = (int)r[0];
		System.out.println("biz:"+biz);
		
		byte[] bId = new byte[8];
		for(int i = 1;i<9;i++){
			bId[i-1]=r[i];
		}
		
		long id = NumberByte.byte2Long(bId);
		System.out.println("id:"+id);
		System.out.println("ml:"+r[9]);
		int ml = (int)r[9];
		byte[] bmsg = new byte[ml];
		
		System.out.println("ml:"+ml);
		for(int i=0;i<ml;i++){
			bmsg[i] = r[10+i];
		}
		try {
			String msg = new String(bmsg,"utf8");
			System.out.println(msg);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
		byte[] a = genDatagram4Long(1,-30,"哈哈");
		System.out.println(ByteUtil.toHex(a));
		System.out.println("--------------------------");
		
		printDatagram4Websocket(a);
	}
}
