package com.blackcrystalinfo.platform.server;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.LogType;
import com.blackcrystalinfo.platform.powersocket.bo.User;
import com.blackcrystalinfo.platform.powersocket.log.ILogger;
import com.blackcrystalinfo.platform.service.IUserSvr;
import com.blackcrystalinfo.platform.util.sms.SMSSender;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

//@Repository
public class SmsScanner {
	private static final Logger logger = LoggerFactory.getLogger(SmsScanner.class);

//	@Autowired
	IUserSvr usrSvr;
//	@Autowired
	ILogger log;
//	@PostConstruct
	public void tiktok() {
//		Thread t = new Thread(){
//			public void run() {
//				scan();
//			}
//		};
//		t.start();
		Constants.TH.submit(new Runnable(){
			@Override
			public void run() {
				scan();
			}
			
		});
	}
	private void scan() {
		logger.info("SMSScanner Launched.{}",usrSvr);
		Jedis j = null;
		try{
			j=DataHelper.getJedis();
			j.subscribe(new JedisPubSub() {
				@Override
				public void onMessage(String channel, String message) {
					logger.info("{}-{}",channel,message);
					try{
						User user = usrSvr.getUser(User.UserIDColumn, message);
						String phone = user.getPhone();
						SMSSender.send(phone, "您的账号已在别处登录，请确认是否为本人操作。");
						log.write(String.format("%s|%s|%s|%s", message,System.currentTimeMillis(),LogType.ZCDL,"您的账号已在别处登录，请确认是否为本人操作。"));
					}catch(Throwable e){
						logger.error("",e);
					}finally{
					}
				}
			}, "SMS");
		}catch(Throwable e){
			
		}finally{
			DataHelper.returnJedis(j);
		}
	}
}
