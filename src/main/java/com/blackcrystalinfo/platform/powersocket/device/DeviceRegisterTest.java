package com.blackcrystalinfo.platform.powersocket.device;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Controller;

import com.blackcrystalinfo.platform.common.NumberByte;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;
import com.blackcrystalinfo.platform.service.InternalException;

@Controller("/api/device/register2")
public class DeviceRegisterTest extends HandlerAdapter {


	public Object rpc(RpcRequest req) throws InternalException {

		String mac = req.getParameter("mac");
		String sn = req.getParameter("sn");
		String dv = req.getParameter("dv");
		String pid = req.getParameter("pid");
		String name = req.getParameter("name");
		String sign = req.getParameter("sign");
		String isUnbind = req.getParameter("doUnbind");
		return deal(mac, sn, dv, pid, name, sign, isUnbind);
	}

	private Object deal(String... args) throws InternalException{
		Map<Object, Object> r = new HashMap<Object, Object>();
		Long l;
		try {
			l = NumberByte.byte2LongLittleEndian(Hex.decodeHex(args[0].toCharArray()));
			r.put("id", l*-1);
		} catch (DecoderException e) {
			e.printStackTrace();
		}
		r.put("cookie", "0000000000000000000000000000000000000000000000000000000000000000");
		r.put("status", 0);
		return r;
	}

	public static void main(String[] args) throws Exception {
		String s = "0100000000000000";
		Hex.decodeHex(s.toCharArray());
		Long l=NumberByte.byte2LongLittleEndian(Hex.decodeHex(s.toCharArray()));
		System.out.println(l);
	}
}
