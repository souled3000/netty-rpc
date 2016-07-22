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

@Controller("/api/device/login2")
public class DeviceLoginTest extends HandlerAdapter {
	public Object rpc(RpcRequest req) throws InternalException {
		String mac = req.getParameter("mac");
		String pid = req.getParameter("pid");
		String cookie = req.getParameter("cookie");
		return deal(mac, pid, cookie);
	}

	private Object deal(String... args) throws InternalException {
		Map<Object, Object> r = new HashMap<Object, Object>();
		Long l;
		try {
			l = NumberByte.byte2LongLittleEndian(Hex.decodeHex(args[0].toCharArray()));
			r.put("id", l * -1);
			r.put("owner", l);
		} catch (DecoderException e) {
			e.printStackTrace();
		}
		r.put("tmp", Hex.encodeHexString(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }));
		r.put("status", 0);
		return r;
	}
}
