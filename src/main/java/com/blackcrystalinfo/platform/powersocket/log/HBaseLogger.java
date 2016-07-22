package com.blackcrystalinfo.platform.powersocket.log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
//@Repository
public class HBaseLogger implements ILogger {

	private Table t;
	private final static BlockingQueue<String> q = new LinkedBlockingQueue<String>();
	
	public HBaseLogger() {
		try {
			t=DataHelper.getHCon().getTable(TableName.valueOf("LOG"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Constants.TH.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				for(;;){
					String log = q.take();
					writeHB(log);
				}
			}
			
		});
	}
	public void write(String log) {
		try {
			q.put(log);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void writeHB(String log){
		String[] fields=log.split("\\|");
		StringBuilder rk = new StringBuilder();
		rk.append(fields[0]);
		rk.append(":");
		rk.append(Long.MAX_VALUE-Long.valueOf(fields[1]));
		Put put = new Put(Bytes.toBytes(rk.toString()), System.currentTimeMillis());
		put.addColumn(Bytes.toBytes("F"), Bytes.toBytes("Q"), Bytes.toBytes(log));
		try {
			this.t.put(put);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
