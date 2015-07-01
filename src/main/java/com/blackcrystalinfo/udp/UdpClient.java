package com.blackcrystalinfo.udp;

import static java.lang.System.out;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.EndianUtils;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class UdpClient {
	public static short ChecksumHeader(short[] msg, int n) {
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

	public static void main(String[] args) throws Exception {
		char a = 2;
		System.out.println(a);
		byte FINMASK = (byte) 0x80;
		System.out.println(FINMASK);
		System.out.println(0x80);
		byte[] b = new byte[] { 1, 3, 127 };
		System.out.println(Arrays.toString(b));
		System.out.println(ByteUtil.byte2String(b));
		System.out.println(Integer.reverseBytes(1));
		System.out.println(Integer.toBinaryString(Integer.reverseBytes(1)));
		System.out.println(Integer.toBinaryString(Integer.reverse(1)));
		System.out.println(Integer.bitCount(9));
		int z = 1 >>> 1;
		int y = -1 >> 1;
		System.out.println(z);
		System.out.println(y);
		out.printf("%#x\n", (byte) y);
		System.out.println(EndianUtils.swapInteger(1));
		System.out.println(Integer.reverseBytes(1));
		out.println((int) (1 << 16));
		UUID uuid = UUID.randomUUID();
		
		out.println(uuid.toString());
		out.print(Long.toHexString(uuid.getMostSignificantBits()));
		out.println(Long.toHexString(uuid.getLeastSignificantBits()));
//		out.println(ByteUtil.toHex(ByteUtil.fromHex(uuid.toString())));
		
		out.println(ByteUtil.toHex(ByteUtil.fromHex(uuid.toString().replaceAll("-", ""))));
		
		byte[] dest = new byte[24];
		System.arraycopy(new byte[] { 1, 2, 3 }, 0, dest, 2, 2);
		int u = 1;
		EndianUtils.writeSwappedInteger(dest, 5, u);
		out.println(ByteUtil.toHex(dest));

		FrameHeader fh = new FrameHeader();
		fh.FIN = 1;
		fh.Ver = 3;
		fh.Opcode = 3;
		fh.Sequence = 0x0103;
		fh.Timestamp = 0x06050403;
		fh.Dst = 0x0F000000000000E0l;
		fh.Src = 0x0700000000000050l;
		fh.GUID=ByteUtil.fromHex(uuid.toString().replaceAll("-", ""));
		byte[] ctn = fh.gen();
		out.println(ByteUtil.toHex(ctn));
		int w = -4096;
		byte[] ww = new byte[4];
		EndianUtils.writeSwappedInteger(ww, 0, w);
		out.println(ByteUtil.toHex(ww));	
		
		UUID sid = UUID.fromString("84269381-8783-471b-524a-d9e147f94e17");
		
		out.println(sid.toString());
		
		short[] ba = new short[]{2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 224, 0, 24, 0, 247, 17, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		short rba =ChecksumHeader(ba, 32);
		out.println(rba);
		out.println((byte)rba);
		int xrba = rba;
		out.println(Integer.toBinaryString(xrba));
		xrba = (byte)rba;
		out.println(Integer.toBinaryString(xrba));
		
	}
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
	public short Sequence;
	public int Timestamp;
	public long Dst;
	public long Src;
	public byte[] GUID = new byte[16];

	public byte[] gen() throws Exception {
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
			throw new Exception("Opcode just can be two value: 2 & 3");
		}

		this.Head1 = (byte) (this.FIN << this.FINOFFSET | this.MASK << this.MASKOFFSET | this.Ver << this.VerOFFSET | this.Opcode);
		System.arraycopy(new byte[] { this.Head1 }, 0, dest, 0, 1);
		System.arraycopy(new byte[] { this.Reserver }, 0, dest, 1, 1);
		EndianUtils.writeSwappedShort(dest, 2, Sequence);
		EndianUtils.writeSwappedInteger(dest, 4, Timestamp);
		EndianUtils.writeSwappedLong(dest, 8, Dst);
		EndianUtils.writeSwappedLong(dest, 16, Src);
		return dest;
	}
}

class DataHeader {
	byte Read;
	final byte ReadMask = (byte) 0x80;
	final byte ReadOffset = 7;
	
	byte ACK;
	final byte ACKMask = (byte) 0x40;
	final byte ACKOffset = 6;
	
	byte DataFormat;
	final byte DataFormatMask = (byte) 0x30;
	final byte DataFormatOffset = 4;
	
	byte KeyLevel;
	final byte KeyLevelMask = (byte) 0x0C;
	final byte KeyLevelOffset = (byte) 0x02;
	
	byte EncryptType;
	final byte EncryptTypeMask = (byte) 0x03;

	byte Flags;

	byte DataSequence;
	short DevType;
	short MsgID;
	short Lenght;
	byte HeadCheckSum;
	byte BodyCheckSum;
	short SessionId;

	public byte[] gen() {
		byte[] dest = null;
		this.Flags = (byte) (this.Read << this.ReadOffset | this.ACK << this.ACKOffset | this.DataFormat << this.DataFormatOffset | this.KeyLevel<<this.KeyLevelOffset|this.EncryptType);
		System.arraycopy(new byte[] { this.Flags }, 0, dest, 0, 1);
		System.arraycopy(new byte[] { this.DataSequence }, 0, dest, 1, 1);
		EndianUtils.writeSwappedShort(dest, 2, DevType);
		EndianUtils.writeSwappedInteger(dest, 4, MsgID);
		EndianUtils.writeSwappedLong(dest, 6, Lenght);
		System.arraycopy(new byte[] { this.HeadCheckSum }, 0, dest, 8, 1);
		System.arraycopy(new byte[] { this.BodyCheckSum }, 0, dest, 9, 1);
		EndianUtils.writeSwappedLong(dest, 10, SessionId);
		return dest;
	}
}

class RequestDatagram {
	FrameHeader fh;
	DataHeader dh;

	public byte[] genTokenRequestDatagram() {
		return null;
	}
}