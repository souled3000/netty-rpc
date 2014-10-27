package com.blackcrystalinfo.platform.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CometScannerV2 {
	private static final Logger logger = LoggerFactory.getLogger(CometScannerV2.class);

	private final static float CPU_THRESHOLD = Float.valueOf(Constants.getProperty("cpu.threshold", "0.9"));
	private final static long MEM_THRESHOLD = Long.valueOf(Constants.getProperty("mem.threshold", "1024"));
	private final static long HANDLER_THRESHOLD = Long.valueOf(Constants.getProperty("handler.threshold", "65535"));

	private final static String root = Constants.getProperty("root", "/");

	private final static BlockingQueue<String> q = new LinkedBlockingQueue<String>();
	private final static Map<String, String> PATH2URL = new HashMap<String, String>();
	private final static Map<String, String> IPMAP = new HashMap<String, String>();

	private static ZooKeeper zk = null;

	private final static Pattern p = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

	static {
		try {
			Properties p = new Properties();
			p.load(ClassLoader.getSystemResourceAsStream("ip.properties"));
			for (Object key : p.keySet()) {
				String v = p.getProperty((String) key);
				IPMAP.put((String) key, v);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean assignZk() {
		if (zk != null && zk.getState().isAlive() && zk.getState().isConnected())
			return true;
		String zookeepers = Constants.getProperty("zookeeper", "");

		logger.debug("connect...{} ", zookeepers);
		try {
			final CountDownLatch signal = new CountDownLatch(1);
			zk = new ZooKeeper(zookeepers, Integer.MAX_VALUE, new Watcher() {

				public void process(WatchedEvent event) {
					if (event.getState() == KeeperState.SyncConnected) {
						if (signal != null) {
							signal.countDown();
						}
					} else if (event.getState() == KeeperState.Expired) {
						System.out.println("[SUC-CORE] session expired. now rebuilding");

						// session expired, may be never happending.
						// close old client and rebuild new client
						CometScannerV2.closing();

						CometScannerV2.assignZk();
					}
				}

			});
			signal.await();
			return true;
		} catch (InterruptedException e) {
			return false;
		} catch (IOException e) {
			logger.error("assignZk IOException: ", e);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			CometScannerV2.closing();
			return assignZk();
		}
	}

	public static void scan() {

		try {
			if (!assignZk()) {
				logger.debug("There is not single one available zookeeper. This turn have to terminate.");
				return;
			}
			searching(zk, root);
		} catch (Exception e) {
			logger.error("", e);
		} finally {

		}
	}

	private static void searching(final ZooKeeper zk, final String path) throws Exception {
		logger.debug("visit znode:{}", path);
		List<String> l = null;
		l = zk.getChildren(path, new Watcher() {
			public void process(WatchedEvent event) {
				logger.debug("path:{}|type:{}|state:{}", event.getPath(), event.getType().toString(), event.getState().toString());
				scan();
			}
		});
		StringBuilder sb = new StringBuilder();
		for (String s : l) {
			Stat st = new Stat();
			final String tmpPath = path.equals("/") ? "/" + s : path + "/" + s;
			final String data = new String(zk.getData(tmpPath, new Watcher() {

				@Override
				public void process(WatchedEvent event) {
					logger.debug("path:{}|type:{}|state:{}", event.getPath(), event.getType().toString(), event.getState().toString());
					if (event.getType().equals(EventType.NodeDeleted)) {
						String url = PATH2URL.get(event.getPath());
						if (url != null)
							q.remove(url);
						logger.debug("NodeDeleted");
					}
					if (event.getType().equals(EventType.NodeDataChanged)) {
						String url = PATH2URL.get(event.getPath());
						if (url != null)
							q.remove(url);
						logger.debug("NodeDataChanged");
						scan();
					}
				}
			}, st));
			assignQueue(data, tmpPath);
			sb.append(String.format("path:%s|data:%s\n", tmpPath, data));
		}
		logger.debug(sb.toString());
	}

	private static void assignQueue(String data, String tmpPath) throws Exception {
		String[] items = data.split(",");
		if (items.length == 5) {
			String url = items[0];
			PATH2URL.put(tmpPath, url);
			float cpu = Float.valueOf(items[1]);
			long tm = Long.valueOf(items[2]);
			long um = Long.valueOf(items[3]);
			long h = Long.valueOf(items[4]);
			if (cpu < CPU_THRESHOLD && tm - um > MEM_THRESHOLD && HANDLER_THRESHOLD > h) {
				if (!q.contains(url)) {
					q.put(url);
					logger.debug("adding url:{}|path:{}", url, tmpPath);
				}
			} else {
				if (q.contains(url)) {
					q.remove(url);
					logger.debug("adding url:{}|path:{}", url, tmpPath);
				}
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
				url = url.replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", IPMAP.get(key));
			else
				logger.error("ip {} has no its mapping ip ", key);
		}
		logger.info("return url:{}", url);
		return url;
	}

	private static void closing() {
		if (!(zk != null && zk.getState().isAlive() && zk.getState().isConnected()))
			try {
				zk.close();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public static void main(String[] args) throws Exception {
		CometScannerV2.scan();
		Thread.sleep(1 * 1000);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				CometScannerV2.closing();
			}
		});
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				String url = CometScannerV2.take();
				System.out.printf("size:%s|%s", q.size(), url);
				System.out.printf(" PATH2URL:%s|%s", PATH2URL.size(), url);
			}
		}, 0, 10000);
	}
}