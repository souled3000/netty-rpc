package com.blackcrystalinfo.platform.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

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

	private final static String root = Constants.getProperty("root", "/");

	private final static BlockingQueue<String> q = new LinkedBlockingQueue<String>();
	private final static Map<String,String> PATH2URL = new HashMap<String,String>();
	
	private static ZooKeeper zk = null;

	public static void tiktok() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				scan();
			}
		}, 0, 1000);
	}

	private static boolean assignZk() {
		String zookeepers = Constants.getProperty("zookeeper", "");

		logger.info("connect...{} ", zookeepers);
		try {
			final CountDownLatch signal = new CountDownLatch(1);
			zk = new ZooKeeper(zookeepers, 1000, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					signal.countDown();
				}
			});
			signal.await();
			return true;
		} catch (IOException | InterruptedException e) {
			zk = null;
			logger.error("", e);
		}
		return false;
	}

	private static void scan() {

		try {
			if (!assignZk()) {
				logger.info("There is not single one available zookeeper. This turn have to terminate.");
				return;
			}
			recurseZnode(zk, root);
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			if (zk != null)
				try {
					zk.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}

	private static void recurseZnode(ZooKeeper zk, String path) {
		logger.info("visit znode:{}", path);
		List<String> l = null;
		try {
			l = zk.getChildren(path, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					
				}
			});
		} catch (KeeperException | InterruptedException e) {
			logger.error("", e);
			return;
		}
		for (String s : l) {
			Stat st = new Stat();
			try {
				String data = new String(zk.getData(path.equals("/") ? "/" + s : path + "/" + s, new Watcher(){

					@Override
					public void process(WatchedEvent event) {
						if(event.getType().equals(EventType.NodeDeleted)){
							String url = PATH2URL.get(event.getPath());
							if(url != null)
							q.remove(url);
						}
					}
					
				}, st));
				String[] items = data.split(",");
				if (items.length == 5) {
					String url = items[0];
					PATH2URL.put(path, url);
					float cpu = Float.valueOf(items[1]);
					long tm = Long.valueOf(items[2]);
					long um = Long.valueOf(items[3]);
					long h = Long.valueOf(items[4]);
					if (cpu < CPU_THRESHOLD && tm - um > MEM_THRESHOLD && HANDLER_THRESHOLD > h) {
						if (!q.contains(url)) {
							q.put(url);
							System.out.println("------------------------------------" + url);
						}
					} else {
						if (q.contains(url))
							q.remove(url);
					}
				}
			} catch (Exception e) {
				return;
			}

			int n = st.getNumChildren();
			logger.info("znode:{} has {} children", path.equals("/") ? "/" + s : path + "/" + s, n);
			if (n > 0) {
				recurseZnode(zk, path.equals("/") ? "/" + s : path + "/" + s);
			} else {
				return;
			}
		}
	}

	public static synchronized String take() {
		String url = Constants.getProperty("websocket.addr", "ws://192.168.2.14:1234/ws");
		url = q.poll();
		if(url!=null)
		q.offer(url);
		return url;
	}

	public static void main(String[] args) throws Exception {
		CometScanner.tiktok();
		Thread.sleep(1 * 1000);

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				String url = CometScanner.take();
				System.out.println("--------------" + url);
			}
		}, 0, 3000);

	}

}