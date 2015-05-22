package redis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.util.DataHelper;


public class RedisTest {

	public static void main(String[] args) throws Exception{
		f2();
	}
	
	
	public static void f2() throws Exception{
		long tt = System.currentTimeMillis();
		
		ExecutorService p = Executors.newCachedThreadPool();
		
		final int loop = 100;
		int threadsum = 10000;
//		final int loop = 100;
//		int threadsum = 10000;

		class CC implements Callable<Long>{
			public int start;
			public CC(int n){
				start = n*loop+1;
			}
			@Override
			public Long call() throws Exception {
				
				Jedis j = DataHelper.getJedis();
				long l = System.currentTimeMillis();
				try{
					for(int i = start;i<=start+loop;i++){
						j.hset("user:email", String.valueOf(i), "XXXXXXXXXXX@XXXXXXXXXXXXX.COM");
						j.hset("user:emailtoid", "XXXXXXXXXXX@XXXXXXXXXXXXXX.COM"+i, String.valueOf(i));
						j.hset("user:nick", String.valueOf(i), "XXXXXXXXXXX@XXXXXXXXXXXXX.COM");
						j.hset("user:pwd", String.valueOf(i), "1000:5b42403737323637323663:401b603aab399ba4b2b9fb10a35f95f13bbf92691bfda08390d7038b0784f7e2b2e5491011f6ee5daf726bbc18f6ed61f4958e355892281ad0b66b34ee35f0d1");
						j.hset("user:phone", String.valueOf(i), "19029394992");
						j.hset("user:family", String.valueOf(i), String.valueOf(i));
					}
				}catch(Exception e){
					DataHelper.returnBrokenJedis(j);
				}finally{
					DataHelper.returnJedis(j);
				}
				l = System.currentTimeMillis() - l;
				System.out.println(""+(l/1000)+"秒");
				return l;
			}
			
		}
		

		List<Future<Long>> l = new ArrayList<Future<Long>>();
		
		for(int i = 0;i<threadsum;i++){
			l.add(p.submit(new CC(i)));
		}
		p.shutdown();
		p.awaitTermination(1, TimeUnit.DAYS);
		
		Long t =  System.currentTimeMillis()-tt;
		System.out.printf("%d秒  %d分   %d时",t/1000,t/1000/60,t/1000/60/60);
		
	}
	public static void f(){
		BigDecimal b = new BigDecimal(0);
		int email = 30;
		int nick = 30;
		int pwd = "1000:5b42403737323637323663:401b603aab399ba4b2b9fb10a35f95f13bbf92691bfda08390d7038b0784f7e2b2e5491011f6ee5daf726bbc18f6ed61f4958e355892281ad0b66b34ee35f0d1".length();
		int phone = 13;
		int emailtoid = 8;
		int family = 8;
		int total = email+nick+pwd+phone+emailtoid+family;
		
		BigDecimal t1 = new BigDecimal(1024);
			b = new BigDecimal(total*1000000);
		System.out.println(pwd);
		System.out.println(total);
		System.out.println(b.toString());
		System.out.println(b.divide(t1).divide(t1)+"M");
	}
	
	public static void f3(){
		int mac = "AAAAAQAAAGQ=".length();
		int mactoid = 8;
		int sn = "65766963654e616d6500000100000000".length();
		int dv = 2;
		int pid = 8;
		int name = 20;
		int owner = 8;
		int total = mac+mactoid+sn+dv+pid+name+owner;
		
		BigDecimal t1 = new BigDecimal(1024);
		
		BigDecimal b = new BigDecimal(total*10000000);
		
		
		System.out.println(mac);
		System.out.println(total);
		System.out.println(b.toString());
		System.out.println(b.divide(t1).divide(t1)+"M");
	}

}
