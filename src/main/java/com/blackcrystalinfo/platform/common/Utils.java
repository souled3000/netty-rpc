package com.blackcrystalinfo.platform.common;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.alibaba.fastjson.JSONArray;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class Utils {

	public static byte[] genMsg(String head, int bizCode, long id, String msg) {
		return ArrayUtils.addAll(head.getBytes(), genDatagram4Long(bizCode, id, msg));
	}

	public static byte[] genMsg(String head, int bizCode, String mac, String msg) {
		return ArrayUtils.addAll(head.getBytes(), genDatagram4Mac(bizCode, mac, msg));
	}

	public static byte[] genDatagram4Long(int bizCode, long id, String msg) {

		try {
			byte[] m;
			m = msg.getBytes("utf8");
			int ml = m.length;
			byte[] r = new byte[10 + ml];
			r[9] = (byte) ml;
			byte[] bId = NumberByte.long2ByteLittleEndian(id);
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

		int biz = (int) r[0];
		System.out.println("biz:" + biz);

		byte[] bId = new byte[8];
		for (int i = 1; i < 9; i++) {
			bId[i - 1] = r[i];
		}

		long id = NumberByte.byte2Long(bId);
		System.out.println("id:" + id);
		System.out.println("ml:" + r[9]);
		int ml = (int) r[9];
		byte[] bmsg = new byte[ml];

		System.out.println("ml:" + ml);
		for (int i = 0; i < ml; i++) {
			bmsg[i] = r[10 + i];
		}
		try {
			String msg = new String(bmsg, "utf8");
			System.out.println(msg);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	public static void main2(String[] args) {
		byte[] a = genDatagram4Long(1, -30, "哈哈");
		System.out.println(ByteUtil.toHex(a));
		System.out.println("--------------------------");
		printDatagram4Websocket(a);
	}

	public static void main(String[] args) throws Exception {
		String s = "[[\"ALL\",113],[\"1\",-78],[\"2\",18],[\"3\",12],[\"4\",50],[\"5\",73],[\"6\",49],[\"7\",9],[\"8\",57],[\"9\",33]]";

		JSONArray jr = JSONArray.parseArray(s);
		for (Object o : jr.toArray()) {
			JSONArray j = (JSONArray) o;
			System.out.println(j.getString(0) + " " + j.getLong(1));
		}
		
		
		class C1{
			public C1() {
			}
			public C1(String name,String age){
				this.name=name;
				this.age=age;
			}
			String name;
			String age;
			public String getName() {
				return name;
			}
			public void setName(String name) {
				this.name = name;
			}
			public String getAge() {
				return age;
			}
			public void setAge(String age) {
				this.age = age;
			}
		}
		
		class C2{
			String book;

			public String getBook() {
				return book;
			}

			public void setBook(String book) {
				this.book = book;
			}
		}
		
		
		C1 c1 = new C1();
		c1.name="helena";
		c1.age="11";
		C2 c2 = new C2();
		c2.book="biblle";
		List l = new ArrayList();
		l.add(c1);
		l.add(c2);
		
		s = JSONArray.toJSONString(l);
		System.out.println(s);
		
		
		l = JSONArray.parseArray(s, new Type[]{C1.class,C2.class});
		
		for (Object o:l){
			if (o instanceof C1){
				System.out.println(((C1) o).getName()+":"+((C1) o).getAge());
			}
			if (o instanceof C2){
				System.out.println(((C2) o).getBook());
			}
		}
		
		l=new ArrayList();
		
		l.add(new C1("helena","33"));
		l.add(new C1("xaviera","22"));
		l.add(new C1("leon","38"));
		
		s= JSONArray.toJSONString(l);
		
		System.out.println(s);
		System.out.println();
		System.out.println();
		
		l = JSONArray.parseArray(s, C1.class);
		for (Object o : l){
			System.out.println(((C1) o).getName()+":"+((C1) o).getAge());
		}
		
		
		
		
		// Jedis j = DataHelper.getJedis();
		// j.publish(Constants.COMMONMSGCODE.getBytes(), Utils.genMsg("-32,-48|", 5, Integer.parseInt("48"), "黄河"));
		// DataHelper.returnJedis(j);
	}
}
