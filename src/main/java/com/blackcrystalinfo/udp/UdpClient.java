package com.blackcrystalinfo.udp;

import static java.lang.System.out;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.lang.ArrayUtils;

import com.blackcrystalinfo.platform.util.NumberByte;
import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class UdpClient {
	public static short ChecksumHeader(byte[] ctn, int n) {
		short[] msg = new short[ctn.length];
		for (int i = 0; i < ctn.length; i++) {
			msg[i] = ctn[i];
		}
		short headerCheck = 0;
		for (int i = 0; i < n; i++) {
			headerCheck ^= msg[i];
			for (int bit = 8; bit > 0; bit--) {
				if ((headerCheck & 0x80) != 0) {
					headerCheck = (short) ((headerCheck << 1) ^ 0x131);
				} else {
					headerCheck = (short) (headerCheck << 1);
				}
			}
		}
		return headerCheck;
	}

	class FrameHeader {
		public byte FIN;
		final byte FINMASK = (byte) 0x80;
		final byte FINOFFSET = 7;
		public byte MASK;
		final byte MASKMASK = (byte) 0x40;
		final byte MASKOFFSET = 5;
		public byte Ver;
		final byte VerMASK = (byte) 0x38;
		final byte VerOFFSET = 3;
		public byte Opcode;
		final byte OpcodeMask = (byte) 0x07;

		public byte Head1;
		public byte Reserver;
		public short PackNum;
		public int Timestamp;
		public long Dst;
		public long Src;
		public byte[] GUID = new byte[16];

		public byte[] gen() {
			byte[] dest = null;
			switch (this.Opcode) {
			case 2:
				dest = new byte[24];
				break;
			case 3:
				dest = new byte[40];
				System.arraycopy(GUID, 0, dest, 24, 16);
				break;
			default:
				return null;
			}

			this.Head1 = (byte) (this.FIN << this.FINOFFSET | this.MASK << this.MASKOFFSET | this.Ver << this.VerOFFSET | this.Opcode);
			System.arraycopy(new byte[] { this.Head1 }, 0, dest, 0, 1);
			System.arraycopy(new byte[] { this.Reserver }, 0, dest, 1, 1);
			EndianUtils.writeSwappedShort(dest, 2, PackNum);
			EndianUtils.writeSwappedInteger(dest, 4, Timestamp);
			EndianUtils.writeSwappedLong(dest, 8, Dst);
			EndianUtils.writeSwappedLong(dest, 16, Src);
			return dest;
		}
	}

	class DataHeader {
		public byte Read;
		final byte ReadMask = (byte) 0x80;
		final byte ReadOffset = 7;

		public byte ACK;
		final byte ACKMask = (byte) 0x40;
		final byte ACKOffset = 6;

		public byte DataFormat;
		final byte DataFormatMask = (byte) 0x30;
		final byte DataFormatOffset = 4;

		public byte KeyLevel;
		final byte KeyLevelMask = (byte) 0x0C;
		final byte KeyLevelOffset = 2;

		public byte EncryptType;
		final byte EncryptTypeMask = (byte) 0x03;

		public byte Flags;

		public byte DataSequence;
		public short DevType;
		public short MsgID;
		public short Lenght;
		public byte HeadCheckSum;
		public byte BodyCheckSum;
		public short SessionId;

		public byte[] gen() {
			byte[] dest = new byte[12];
			this.Flags = (byte) (this.Read << this.ReadOffset | this.ACK << this.ACKOffset | this.DataFormat << this.DataFormatOffset | this.KeyLevel << this.KeyLevelOffset | this.EncryptType);
			System.arraycopy(new byte[] { this.Flags }, 0, dest, 0, 1);
			System.arraycopy(new byte[] { this.DataSequence }, 0, dest, 1, 1);
			EndianUtils.writeSwappedShort(dest, 2, DevType);
			EndianUtils.writeSwappedShort(dest, 4, MsgID);
			EndianUtils.writeSwappedShort(dest, 6, Lenght);
			System.arraycopy(new byte[] { this.HeadCheckSum }, 0, dest, 8, 1);
			System.arraycopy(new byte[] { this.BodyCheckSum }, 0, dest, 9, 1);
			EndianUtils.writeSwappedShort(dest, 10, SessionId);
			return dest;
		}
	}

	class RequestDatagram {
		private AtomicInteger atom = new AtomicInteger(-1);

		public byte[] MAC = new byte[8];
		public byte[] SN = new byte[16];
		public byte[] SIGN = new byte[260];
		public byte[] dv = new byte[2];
		public byte[] name;
		public byte[] produceTime = new byte[4];
		public byte[] sid = new byte[16];
		public byte[] cookie = new byte[64];

		public byte[] deviceId;

		public short gotPackNum() {
			if (atom.get() == Short.MAX_VALUE - 10) {
				atom.set(0);
			} else {
				return (short) atom.addAndGet(1);
			}
			return 0;
		}

		public byte[] genForwardDatagram(long dstId, byte[] content) {
			byte[] pk = new byte[24 + 16 + 12 + content.length];
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 3;
			fh.Dst = dstId;
			fh.Src = NumberByte.byte2LongLittleEndian(this.deviceId);
			fh.PackNum = gotPackNum();
			fh.Timestamp = (int) System.currentTimeMillis();
			fh.GUID = this.sid;
			byte[] ctn = content;
			System.arraycopy(ctn, 0, pk, 36, ctn.length);
			System.arraycopy(fh.gen(), 0, pk, 0, 24 + 16);
			System.arraycopy(dh.gen(), 0, pk, 24 + 16, 12);
			dh.HeadCheckSum = (byte) ChecksumHeader(pk, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(pk, 34, pk.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, pk, 0, 24 + 16);
			System.arraycopy(dh.gen(), 0, pk, 24 + 16, 12);

			return pk;
		}

		public byte[] genTokenRequestDatagram() {
			byte[] pk = new byte[24 + 12 + 24];
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 2;
			fh.PackNum = gotPackNum();
			fh.Timestamp = (int) System.currentTimeMillis();
			dh.MsgID = 0xE0;
			dh.Lenght = 24;
			byte[] ctn = new byte[24];
			System.arraycopy(MAC, 0, ctn, 0, 8);
			System.arraycopy(SN, 0, ctn, 8, 16);
			System.arraycopy(fh.gen(), 0, pk, 0, 24);
			System.arraycopy(dh.gen(), 0, pk, 24, 12);
			System.arraycopy(ctn, 0, pk, 36, 24);
			dh.HeadCheckSum = (byte) ChecksumHeader(pk, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(pk, 34, pk.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, pk, 0, 24);
			System.arraycopy(dh.gen(), 0, pk, 24, 12);

			return pk;
		}

		public byte[] genRegister2Datagram() {
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 2;
			fh.PackNum = gotPackNum();
			fh.Timestamp = (int) System.currentTimeMillis();

			dh.MsgID = 0xE1;

			byte[] finalDatagram = new byte[24 + 12 + 309 + this.name.length];
			byte[] ctn = new byte[309 + this.name.length];
			dh.Lenght = (short) (309 + this.name.length);

			System.arraycopy(this.sid, 0, ctn, 0, 16);
			System.arraycopy(this.dv, 0, ctn, 16, 2);
			System.arraycopy(this.produceTime, 0, ctn, 20, 4);
			System.arraycopy(MAC, 0, ctn, 24, 8);
			System.arraycopy(SN, 0, ctn, 32, 16);
			System.arraycopy(new byte[] { (byte) this.name.length }, 0, ctn, 308, 1);
			System.arraycopy(this.name, 0, ctn, 309, this.name.length);
			out.println("--------------------------this.name:" + new String(this.name));
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			System.arraycopy(ctn, 0, finalDatagram, 36, dh.Lenght);
			dh.HeadCheckSum = (byte) ChecksumHeader(finalDatagram, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(finalDatagram, 34, finalDatagram.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			return finalDatagram;
		}

		public byte[] genRegisterDatagram() {
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 2;
			fh.PackNum = gotPackNum();

			dh.MsgID = 0xE1;

			byte[] finalDatagram = new byte[24 + 12 + 296 + this.name.length];
			byte[] ctn = new byte[296 + this.name.length];
			dh.Lenght = (short) (296 + this.name.length);

			System.arraycopy(this.sid, 0, ctn, 0, 16);
			System.arraycopy(this.dv, 0, ctn, 16, 2);
			System.arraycopy(MAC, 0, ctn, 18, 8);
			System.arraycopy(SN, 0, ctn, 26, 8);
			System.arraycopy(SIGN, 0, ctn, 34, 260);
			System.arraycopy(new byte[] { (byte) this.name.length }, 0, ctn, 295, 1);
			System.arraycopy(this.name, 0, ctn, 296, this.name.length);
			out.println("--------------------------this.name:" + new String(this.name));
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			System.arraycopy(ctn, 0, finalDatagram, 36, dh.Lenght);
			dh.HeadCheckSum = (byte) ChecksumHeader(finalDatagram, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(finalDatagram, 34, finalDatagram.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			return finalDatagram;
		}

		public byte[] genLoginDatagram() {
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 2;
			fh.PackNum = gotPackNum();
			fh.Timestamp = (int) System.currentTimeMillis();

			dh.MsgID = 0xE2;

			byte[] finalDatagram = new byte[24 + 12 + 88];
			byte[] ctn = new byte[88];
			dh.Lenght = 88;

			System.arraycopy(this.sid, 0, ctn, 0, 16);
			System.arraycopy(this.MAC, 0, ctn, 16, 8);
			System.arraycopy(cookie, 0, ctn, 24, 64);
			System.arraycopy(ctn, 0, finalDatagram, 36, dh.Lenght);

			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			dh.HeadCheckSum = (byte) ChecksumHeader(finalDatagram, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(finalDatagram, 34, finalDatagram.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);

			return finalDatagram;
		}

		public byte[] genRenameDatagram() {
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 2;
			fh.PackNum = gotPackNum();
			fh.Timestamp = (int) System.currentTimeMillis();

			dh.MsgID = 0xE3;

			byte[] finalDatagram = new byte[24 + 12 + 25 + this.name.length];
			byte[] ctn = new byte[25 + this.name.length];
			dh.Lenght = (short) (25 + this.name.length);

			System.arraycopy(this.sid, 0, ctn, 0, 16);
			System.arraycopy(this.MAC, 0, ctn, 16, 8);
			System.arraycopy(new byte[] { (byte) this.name.length }, 0, ctn, 24, 1);
			System.arraycopy(this.name, 0, ctn, 25, this.name.length);
			System.arraycopy(ctn, 0, finalDatagram, 36, dh.Lenght);

			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			dh.HeadCheckSum = (byte) ChecksumHeader(finalDatagram, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(finalDatagram, 34, finalDatagram.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			return finalDatagram;
		}

		public byte[] genUnbindDatagram() {
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 2;
			fh.PackNum = gotPackNum();
			fh.Timestamp = (int) System.currentTimeMillis();

			dh.MsgID = 0xE4;

			byte[] finalDatagram = new byte[24 + 12 + 25];
			byte[] ctn = new byte[25];
			dh.Lenght = (short) (25);

			System.arraycopy(this.sid, 0, ctn, 0, 16);
			System.arraycopy(this.deviceId, 0, ctn, 16, 8);
			System.arraycopy(ctn, 0, finalDatagram, 36, dh.Lenght);

			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			dh.HeadCheckSum = (byte) ChecksumHeader(finalDatagram, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(finalDatagram, 34, finalDatagram.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			return finalDatagram;
		}

		public byte[] genHeartBeatDatagram() {
			FrameHeader fh = new FrameHeader();
			DataHeader dh = new DataHeader();
			fh.Opcode = 2;
			fh.PackNum = gotPackNum();
			fh.Timestamp = (int) System.currentTimeMillis();

			dh.MsgID = 0xE5;

			byte[] finalDatagram = new byte[24 + 12 + 24];
			byte[] ctn = new byte[24];
			dh.Lenght = (short) (24);

			System.arraycopy(this.sid, 0, ctn, 0, 16);
			System.arraycopy(this.MAC, 0, ctn, 16, 8);
			System.arraycopy(ctn, 0, finalDatagram, 36, dh.Lenght);

			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			dh.HeadCheckSum = (byte) ChecksumHeader(finalDatagram, 32);
			dh.BodyCheckSum = (byte) ChecksumHeader(Arrays.copyOfRange(finalDatagram, 34, finalDatagram.length), dh.Lenght + 2);
			System.arraycopy(fh.gen(), 0, finalDatagram, 0, 24);
			System.arraycopy(dh.gen(), 0, finalDatagram, 24, 12);
			return finalDatagram;
		}

		public byte[] genSubDeviceOfflineDatagram() {
			return null;
		}

	}

	public RequestDatagram request;
	public DatagramSocket client;
	public SocketAddress local;
	public SocketAddress remote;

	public UdpClient(String mac, String name, SocketAddress lc, SocketAddress rm) {
		this.request = new RequestDatagram();
		this.request.dv[1] = 0x02;
		this.request.MAC = ByteUtil.fromHex(mac);
		this.request.name = name.getBytes();
		this.request.SN = ByteUtil.fromHex(UUID.randomUUID().toString().replaceAll("-", ""));
		this.request.produceTime = NumberByte.int2Byte((int) System.currentTimeMillis());
		this.local = lc;
		this.remote = rm;
		try {
			client = new DatagramSocket(local);
			client.connect(remote);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public byte[] sendTokenRequest() throws Exception {
		out.println("-----------------------------------------------------------调用Token");
		byte[] req = this.request.genTokenRequestDatagram();

		DatagramPacket dp = new DatagramPacket(req, req.length);
		client.send(dp);
		DatagramPacket rec = new DatagramPacket(new byte[64], 64);
		client.receive(rec);
		byte[] data = rec.getData();
		out.println("返回:" + ByteUtil.toHex(data));
		this.request.sid = ArrayUtils.subarray(data, 36 + 4, 36 + 20);
		out.println(ByteUtil.toHex(this.request.sid));
		return data;
	}

	public byte[] sendRegisterRequest() throws Exception {
		out.println("--------------------------------------------------------调用注册");
		this.request.name = "李春江".getBytes();
		this.request.dv = new byte[] { 0x00, 0x01 };
		byte[] req = this.request.genRegister2Datagram();
		out.println(ByteUtil.toHex(req));
		DatagramPacket dp = new DatagramPacket(req, req.length);
		client.send(dp);
		DatagramPacket rec = new DatagramPacket(new byte[128], 128);
		client.receive(rec);
		byte[] data = rec.getData();
		out.println("返回:" + ByteUtil.toHex(data));
		this.request.cookie = ArrayUtils.subarray(data, 64, 128);
		out.println("cookie:" + new String(this.request.cookie));
		this.request.deviceId = ArrayUtils.subarray(data, 20 + 36, 28 + 36);
		out.println("deviceId:" + NumberByte.byte2LongLittleEndian(this.request.deviceId));
		int rt = NumberByte.byte2Int(ArrayUtils.subarray(data, 36, 40));
		out.println("返回码:" + rt);
		return data;
	}

	public byte[] sendUnbindRequest() throws Exception {
		out.println("------------------------------------------------------调用解绑");
		byte[] req = this.request.genUnbindDatagram();
		DatagramPacket dp = new DatagramPacket(req, req.length);
		client.send(dp);
		DatagramPacket rec = new DatagramPacket(new byte[128], 128);
		client.receive(rec);
		byte[] data = rec.getData();
		out.println("返回:" + ByteUtil.toHex(data));
		int rt = NumberByte.byte2Int(ArrayUtils.subarray(data, 36, 40));
		out.println("返回码:" + rt);
		this.request.MAC = ArrayUtils.subarray(data, 20 + 36, 28 + 36);
		out.println("MAC:" + NumberByte.byte2LongLittleEndian(this.request.MAC));
		return data;
	}

	public byte[] sendHeartBeatRequest() throws Exception {
		out.println("----------------------------------------------------调用心跳");
		byte[] req = this.request.genHeartBeatDatagram();
		DatagramPacket dp = new DatagramPacket(req, req.length);
		client.send(dp);
		return null;
	}

	public byte[] sendLoginRequest() throws Exception {
		out.println("----------------------------------------------------调用登录");
		byte[] req = this.request.genLoginDatagram();
		DatagramPacket dp = new DatagramPacket(req, req.length);
		client.send(dp);
		DatagramPacket rec = new DatagramPacket(new byte[36 + 21], 36 + 21);
		client.receive(rec);
		byte[] data = rec.getData();
		out.println("返回:" + ByteUtil.toHex(data));
		int rt = NumberByte.byte2Int(ArrayUtils.subarray(data, 36, 40));
		out.println("返回状态:" + rt);
		return data;
	}

	public byte[] sendRenameRequest(String newName) throws Exception {
		out.println("-----------------------------------------------------调用改名");
		this.request.name = newName.getBytes();
		byte[] req = this.request.genRenameDatagram();
		DatagramPacket dp = new DatagramPacket(req, req.length);
		client.send(dp);
		DatagramPacket rec = new DatagramPacket(new byte[36 + 28], 36 + 28);
		client.receive(rec);
		byte[] data = rec.getData();
		out.println("返回:" + ByteUtil.toHex(data));
		int rt = NumberByte.byte2Int(ArrayUtils.subarray(data, 36, 40));
		out.println("返回状态:" + rt);
		String mac = ByteUtil.toHex(ArrayUtils.subarray(data, 56, 64));
		out.println("返回MAC:" + mac);
		return data;
	}

	public void sendForwardRequest(long dstId, byte[] content) throws Exception {
		out.println("----------------------------------------------------调用转发");
		byte[] req = this.request.genForwardDatagram(dstId, content);
		DatagramPacket dp = new DatagramPacket(req, req.length);
		client.send(dp);
	}

	public static void main(String[] args) throws Exception {
		boot();
	}

	public static void f() {
		for (int i = 0; i < 10; i++) {
			System.out.println("\"" + UUID.randomUUID().toString().replaceAll("-", "").toUpperCase() + "\",");
		}
	}

	public final static String[] MACS = new String[] { "EEAE62AE81714BAC8E9E70A9BFE93D0F", "0100000000000000", "D6D2873497AC4E638002023A43889126", "EF384BEEF61E4ADA944485DA6B8FE393", "9C3425EE0DEE4516870CEE07D9C52099",
			"6266630D85A64EC186462FA7E63C9352", "26C803DA5E864A229CFF0010A36F4B07", "CA2F481D050C456488FF0DF920083875", "049DF022B73F47ACBB6CA4080F056917", "F8C5F034C0C741B490067C30BDD703D1", "D7A18B11A64A4DD4A27F953810386FB2" };
	public final static int[] ports = new int[] { 10000, 10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 10009, 10010 };

	public static void boot() throws Exception {
		for (int i = 0; i < MACS.length - 10; i++) {
			SocketAddress lc = new InetSocketAddress("193.168.1.60", ports[i]);
			SocketAddress rm = new InetSocketAddress("193.168.1.60", 7999);
			final UdpClient vender = new UdpClient(MACS[i], "无所谓", lc, rm);
			vender.sendTokenRequest();
			vender.sendRegisterRequest();
			vender.sendLoginRequest();
//			vender.sendRenameRequest("BugattiVeyron" + i);
//			vender.sendUnbindRequest();
//			vender.sendForwardRequest(16L, new byte[40]);
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				public void run() {
					try {
						vender.sendHeartBeatRequest();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 0, 20000);
			final DatagramSocket tc = vender.client;
			new Thread() {
				public void run() {
					while (true) {
						DatagramPacket p = new DatagramPacket(new byte[512], 512);
						try {
							tc.receive(p);
							System.out.println("收到消息:" + ByteUtil.toHex(ArrayUtils.subarray(p.getData(), 0, p.getLength() - 1)));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
			}.start();
		}
	}
}
