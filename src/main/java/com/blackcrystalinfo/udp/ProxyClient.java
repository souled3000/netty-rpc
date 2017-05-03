package com.blackcrystalinfo.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.lang.ArrayUtils;

public class ProxyClient {

	public static void main(String[] args) throws Exception {
		SocketAddress local = new InetSocketAddress("193.168.1.60", 2010);
		SocketAddress remote = new InetSocketAddress("193.168.1.60", 40000);
		DatagramSocket client = new DatagramSocket(local);
		client.connect(remote);
		for (int i = 0; i < 100; i++) {
			client.send(new DatagramPacket(new byte[] { 0x00, 0x00 }, 2));
			DatagramPacket dp = new DatagramPacket(new byte[32], 32);
			client.receive(dp);
			byte[] rtbytes = ArrayUtils.subarray(dp.getData(), 0, dp.getLength());
			String rt = new String(rtbytes);
			System.out.println(rt);
		}
		client.close();
	}

}
