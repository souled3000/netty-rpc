package com.blackcrystalinfo.udp;

import java.util.Arrays;

import oracle.jrockit.jfr.events.Bits;

import com.blackcrystalinfo.platform.util.cryto.ByteUtil;

public class UdpClient {
	public static void main(String[] args) {
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
		int z = 1>>>1;
		int y = -1>>1;
		System.out.println(z);
		System.out.println(y);
		
	}
}

class FrameHeader {
	byte FIN;
	final byte FINMASK = (byte) 0x80;
	byte MASK;
	final byte MASKMASK=(byte)0x60;
	byte Ver;
	final byte VerMASK=(byte)0x38;
	byte Opcode;
	final byte OpcodeMask=(byte)0x07;

	byte Head1;
	byte Reserver;
	byte[] Sequence = new byte[2];
	byte[] Timestamp = new byte[4];
	byte[] Dst = new byte[8];
	byte[] Src = new byte[8];
	byte[] GUID = new byte[2];
	String ss = "";
	
	
}
class DataHeader{
	byte Read;
	final byte ReadMask=(byte)0x80;
	byte ACK;
	final byte ACKMask=(byte)0x60;
	byte DataFormat;
	final byte DataFormatMask=(byte)0x50;
	byte KeyLevel;
	final byte KeyLevelMask=(byte)0x0C;
	byte EncryptType;
	final byte EncryptTypeMask=(byte)0x05;
	
	byte Flags;
	
	byte DataSequence;
	byte[] DevType=new byte[2];
	byte[] MsgID = new byte[2];
	byte[] Lenght=new byte[2];
	byte HeadCheckSum;
	byte BodyCheckSum;
	byte[] SessionId = new byte[2];
}

