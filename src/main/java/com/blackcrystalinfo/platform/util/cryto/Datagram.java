package com.blackcrystalinfo.platform.util.cryto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import com.alibaba.fastjson.JSONObject;

public class Datagram {
	private static final Logger logger = LoggerFactory.getLogger(Datagram.class);
	private BASE64Decoder decoder = new BASE64Decoder();
	private BASE64Encoder encoder = new BASE64Encoder();
	private String key;
	private String ctp;
	private String ktm;
	private String crc;
	private String ctn;

	public Datagram() {
	}

	public Datagram(String key, String ktm, String ctp, String crc, String ctn) {
		this.key = key;
		this.ctn = ctn;
		this.ctp = ctp;
		this.crc = crc;
		this.ktm = ktm;
		logger.info(this.toString());
	}

	public String toString() {
		return String.format("key:%s|ctn:%s|ctp:%s|crc:%s|ktm:%s", key, ctn, ctp, crc, ktm);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getCtp() {
		return ctp;
	}

	public void setCtp(String ctp) {
		this.ctp = ctp;
	}

	public String getKtm() {
		return ktm;
	}

	public void setKtm(String ktm) {
		this.ktm = ktm;
	}

	public String getCrc() {
		return crc;
	}

	public void setCrc(String crc) {
		this.crc = crc;
	}

	public String getCtn() {
		return ctn;
	}

	public void setCtn(String ctn) {
		this.ctn = ctn;
	}

	public void encapsulate() throws Exception {

		Random r = new Random();
		int intKtm = r.nextInt(65535);
		logger.info("intKtm:" + intKtm);
		ktm = StringUtils.leftPad(Integer.toHexString(intKtm), 4, '0');
		// ctp = String.valueOf(r.nextInt(2) + 1);
		ctp = String.valueOf(1);
		Long keyFinal = getFinalKey(key, ktm);

		crc = StringUtils.leftPad(ByteUtil.crc(ctn), 8, '0');

		switch (Integer.valueOf(ctp)) {
		case 1:
			// logger.info("base64--ctn:{}",ctn);
			ctn = encoder.encode(DES.encrypt(ctn.getBytes(), ByteUtil.reverse(ByteUtil.fromHex(Long.toHexString(keyFinal))))).replaceAll("\\s", "");
			// logger.info("base64--ctn:{}",ctn);
			// ctn = URLEncoder.encode(ctn, "utf8");
			// logger.info("urlencoder ctn:{}",ctn);
			break;
		case 2:
			// ctn = decoder.decodeBuffer(RC4.HloveyRC4(ctn.getBytes(), String.valueOf(keyFinal)));
			break;
		default:
			break;
		}
		logger.info("encapsulate: " + this.toString());
	}

	public Long getFinalKey(String key, String ktm) throws Exception {

		Long lKtm = Long.valueOf(ktm, 16);
		Long lKtmLow = lKtm & 0x00ff;
		Long lKtmHight = lKtm & 0xff00;
		lKtmHight >>= 8;

		// logger.info("lKtm:" + lKtm+"|"+Long.toHexString(lKtm));
		// logger.info("lKtmLow:" + lKtmLow+"|"+Long.toHexString(lKtmLow));
		// logger.info("lKtmHight:" + lKtmHight+"|"+Long.toHexString(lKtmHight));

		Long key2 = Long.valueOf(CodePool.getCode(lKtmLow), 16);
		// logger.info("key2:" + key2+"|"+Long.toHexString(key2));

		Long key3 = Long.valueOf(CodePool.getCode(lKtmHight), 16);

		// logger.info("key3:" + key3+"|"+Long.toHexString(key3));

		key3 <<= 32;
		Long key4 = key2 + key3;
		// logger.info("key4:" + key4+"|"+Long.toHexString(key4));
		Long keyFinal = null;
		if (key != null && !"".equals(key)) {
			byte[] md5Key = MessageDigest.getInstance("MD5").digest(key.getBytes());

			// logger.info("md5: " + ByteUtil.toHex(md5Key));
			byte[] md5KeyLower = new byte[8];
			System.arraycopy(md5Key, 0, md5KeyLower, 0, 8);

			ByteBuffer bb = ByteBuffer.allocate(8);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			bb.put(md5KeyLower);
			Long key1 = bb.getLong(0);

			// logger.info("md5的低八:" + ByteUtil.toHex(md5KeyLower));
			// logger.info("key1:" + Long.toHexString(key1));
			// logger.info("key1:" + key1);
			keyFinal = key1 ^ key4;
		} else {
			keyFinal = key4;
		}
		logger.info("keyFinal:{}|{}", keyFinal, Long.toHexString(keyFinal));
		return keyFinal;
	}

	public void decapsulate() throws Exception {
		Long keyFinal = getFinalKey(key, ktm);

		int intCtp = Integer.parseInt(ctp);
		String strCtn = "";
		switch (intCtp) {
		case 1:
			byte[] debase64 = decoder.decodeBuffer(ctn);
			// logger.info("ctn:" + ctn);
			// logger.info("base64 ctn:" + ByteUtil.toHex(debase64));
			// logger.info("keyFinal: " + keyFinal);
			// logger.info("keyFinal: " + Long.toHexString(keyFinal));
			strCtn = new String(DES.decrypt(debase64, ByteUtil.reverse(ByteUtil.fromHex(Long.toHexString(keyFinal)))), "utf8");
			break;
		case 2:
			strCtn = RC4.HloveyRC4(String.valueOf(decoder.decodeBuffer(ctn)), String.valueOf(keyFinal));
			break;
		default:
			logger.info("What!!");
			break;
		}
		logger.info("ctn plaintext：{}", strCtn);

		long c = ByteUtil.crc32(strCtn, crc);
		if (c == 0) {
			ctn = strCtn;
		} else {
			ctn = strCtn;
		}

	}

	public static void main(String[] args) throws Exception {
		Datagram g = new Datagram(null, null, null, null, null);
		g.decapsulate();
		JSONObject.parseObject("mac:\"CFiqNk6cAAA=\",name:\"IOi/meaYr+e9keWFsw==\"}");
	}
}
