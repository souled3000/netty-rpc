package com.blackcrystalinfo.udp;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class Test {

	public static void main(String[] args) {
		String s = "09E69D8EE698A5E6B19F";
		System.out.println(new String(ByteUtil.fromHex(s)));
	}

}
