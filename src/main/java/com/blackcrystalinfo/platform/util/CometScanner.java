package com.blackcrystalinfo.platform.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CometScanner {
	private static final Logger logger = LoggerFactory.getLogger(CometScanner.class);

	private final static float CPU_THRESHOLD = Float.valueOf(Constants.getProperty("cpu.threshold", "0.9"));
	private final static long MEM_THRESHOLD = Long.valueOf(Constants.getProperty("mem.threshold", "1024"));
	private final static long HANDLER_THRESHOLD = Long.valueOf(Constants.getProperty("handler.threshold", "65535"));
	private final static long TICTOK = Long.valueOf(Constants.getProperty("tiktok", "1000"));

	private final static String root = Constants.getProperty("root", "/");

	private final static BlockingQueue<String> q = new LinkedBlockingQueue<String>();
	private final static Map<String, String> PATH2URL = new ConcurrentHashMap<String, String>();
	private final static Map<String, String> IPMAP = new HashMap<String, String>();

	private static volatile ZooKeeper zk = null;

	private final static Pattern p = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+");

	static {
		refresh();
	}

	public static void refresh() {
		try {
			Properties p = new Properties();
			p.load(ClassLoader.getSystemResourceAsStream("ip.properties"));
			for (Object key : p.keySet()) {
				String v = p.getProperty((String) key);
				logger.info("{}---{}", key, v);
				IPMAP.put((String) key, v);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void tiktok() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				scan();
			}
		}, 0, TICTOK);
	}

	private static boolean assignZk() {
		if (zk != null && zk.getState().isAlive() && zk.getState().isConnected())
			return true;
		String zookeepers = Constants.getProperty("zookeeper", "");
		logger.debug("connect...{} ", zookeepers);
		try {
			final CountDownLatch signal = new CountDownLatch(1);
			zk = new ZooKeeper(zookeepers, 5000, new Watcher() {
				public void process(WatchedEvent event) {
					if (signal.getCount() == 1)
						signal.countDown();
					if (event.getType().equals(EventType.NodeDeleted)) {
						String url = PATH2URL.get(event.getPath());
						if (url != null) {
							q.remove(url);
						}
						PATH2URL.remove(url);
						logger.debug("NodeDeleted");
					}
				}
			});
			signal.await();
			return true;
		} catch (Exception e) {
			logger.error("", e);
		}
		return false;
	}

	private static void scan() {

		try {
			if (!assignZk()) {
				logger.debug("There is not single one available zookeeper. This turn have to terminate.");
				return;
			}
			searching(root);
		} catch (KeeperException e) {
			logger.error("scan:KeeperException-", e);
		} catch (InterruptedException e) {
			logger.error("scan:InterruptedException-", e);
		} catch (Exception e) {
			logger.error("scan:Exception-", e);
		} finally {

		}
	}

	private static void searching(String path) throws Exception {
		List<String> l = null;
		l = zk.getChildren(path, false);
		Set<String> curPaths = new HashSet<String>();
		for (String s : l) {
			Stat st = new Stat();
			final String tmpPath = path.equals("/") ? "/" + s : path + "/" + s;
			curPaths.add(tmpPath);
			String data = new String(zk.getData(tmpPath, true, st));
			String[] items = data.split(",");
			if (items.length == 5) {
				String url = items[0];
				PATH2URL.put(tmpPath, url);
				float cpu = Float.valueOf(items[1]);
				/** CPU负荷 */
				long tm = Long.valueOf(items[2]);
				/** 总内存 */
				long um = Long.valueOf(items[3]);
				/** 已用内存 */
				long h = Long.valueOf(items[4]);
				/** 已用句柄数 */
				if (cpu < CPU_THRESHOLD && tm - um > MEM_THRESHOLD && HANDLER_THRESHOLD > h) {
					Matcher m = p.matcher(url);
					if (m.find()) {
						String key = m.group();
						if (!q.contains(url)) {
							if (IPMAP.containsKey(key)) {
								q.put(url);
								logger.debug("adding url:{}|path:{} to q", url, tmpPath);
							} else
								logger.debug("not adding url:{}|path:{} to q, beacause of absent of url in IPMAP", url, tmpPath);
						}else
							logger.debug("not adding url:{}|path:{} to q, the url has been exsisted in q", url, tmpPath);
					}
				} else {
					logger.error("ALARM!!! {} < {}CPU_THRESHOLD && {} - {} > {}MEM_THRESHOLD && {}HANDLER_THRESHOLD > {}", cpu, CPU_THRESHOLD, tm, um, MEM_THRESHOLD, HANDLER_THRESHOLD, h);
					if (q.contains(url)) {
						q.remove(url);
						logger.info("remove url:{}|path:{}", url, tmpPath);
					}
				}
			}
		}

		for (String tmp : PATH2URL.keySet()) {
			if (!curPaths.contains(tmp)) {
				PATH2URL.remove(tmp);
			}
		}
		Map<String,String> tmpMap = new HashMap<String,String>();
		for(String k : PATH2URL.keySet()){
			tmpMap.put(PATH2URL.get(k), k);
		}
		for(String k : q){
			if(tmpMap.get(k)==null){
				q.remove(k);
			}
		}
		
	}

	public static synchronized String take() {
		String url = q.poll();
		if (url != null)
			q.offer(url);
		else
			url = Constants.getProperty("websocket.addr", "");
		Matcher m = p.matcher(url);
		if (m.find()) {
			String key = m.group();
			if (IPMAP.get(key) != null)
				url = url.replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+", IPMAP.get(key));
			else {
				logger.error("ip {} has no its mapping ip ", key);
				url = "";
			}
		}
		logger.debug("return url:{}", url);
		return url;
	}

	public static void closing() {
		if (zk != null)
			try {
				zk.close();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public static void main2(String[] args) throws Exception {
		CometScanner.tiktok();
		Thread.sleep(1 * 1000);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				CometScanner.closing();
			}
		});
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				String url = CometScanner.take();
				System.out.printf("url:%s|qsize:%d|path2urlsize:%d\n", url, q.size(),PATH2URL.size());
			}
		}, 0, 10000);
	}

	public static void main(String[] args) {
		Set<String> a = new HashSet<String>();
		a.add("a");
		a.add("b");
		Set<String> b = new HashSet<String>();
		b.add("a");
		b.add("c");
		float f = Float.valueOf("NaN");
		System.out.println(f < CPU_THRESHOLD);
		System.out.println(Float.valueOf("NaN"));
		
	}
}