package com.blackcrystalinfo.hbase;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm;
import org.apache.hadoop.hbase.util.Bytes;

import com.blackcrystalinfo.platform.common.LogType;

public class Deamon {
	private static final String TABLE_NAME = "MY_TABLE_NAME_TOO";
	private static final String CF_DEFAULT = "DEFAULT_COLUMN_FAMILY";

	public static void createOrOverwrite(Admin admin, HTableDescriptor table) throws IOException {
		if (admin.tableExists(table.getTableName())) {
			admin.disableTable(table.getTableName());
			admin.deleteTable(table.getTableName());
		} else
			admin.createTable(table);
	}

	public static void createSchemaTables(Configuration config) throws IOException {
		try (Connection connection = ConnectionFactory.createConnection(config); Admin admin = connection.getAdmin()) {

			HTableDescriptor table = new HTableDescriptor(TableName.valueOf(TABLE_NAME));
			// table.addFamily(new HColumnDescriptor(CF_DEFAULT).setCompressionType(Algorithm.SNAPPY));
			table.addFamily(new HColumnDescriptor(CF_DEFAULT).setCompressionType(Algorithm.GZ));

			System.out.print("Creating table. ");
			createOrOverwrite(admin, table);
			System.out.println(" Done.");
		}
	}

	public static void modifySchema(Configuration config) throws IOException {
		try (Connection connection = ConnectionFactory.createConnection(config); Admin admin = connection.getAdmin()) {

			TableName tableName = TableName.valueOf(TABLE_NAME);
			if (admin.tableExists(tableName)) {
				System.out.println("Table does not exist.");
				System.exit(-1);
			}

			HTableDescriptor table = new HTableDescriptor(tableName);

			// Update existing table
			HColumnDescriptor newColumn = new HColumnDescriptor("NEWCF");
			newColumn.setCompactionCompressionType(Algorithm.GZ);
			newColumn.setMaxVersions(HConstants.ALL_VERSIONS);
			admin.addColumn(tableName, newColumn);

			// Update existing column family
			HColumnDescriptor existingColumn = new HColumnDescriptor(CF_DEFAULT);
			existingColumn.setCompactionCompressionType(Algorithm.GZ);
			existingColumn.setMaxVersions(HConstants.ALL_VERSIONS);
			table.modifyFamily(existingColumn);
			admin.modifyTable(tableName, table);

			// Disable an existing table
			admin.disableTable(tableName);

			// Delete an existing column family
			admin.deleteColumn(tableName, CF_DEFAULT.getBytes("UTF-8"));

			// Delete a table (Need to be disabled first)
			admin.deleteTable(tableName);
		}
	}

	public static void main1(String... args) throws IOException {
		byte[] a = Bytes.toBytes(1000l);
		System.out.println(a.length);
		byte[] c = Bytes.add(a, a);
		System.out.println(c.length);
		System.out.println(System.currentTimeMillis() - 1020000000);
	}

	public static void main3(String... args) throws IOException {
//		System.setProperty("hadoop.home.dir", "C:\\hadoop-common-2.2.0-bin-master\\");
		Configuration config = HBaseConfiguration.create();
		// config.addResource(new Path(System.getenv("HBASE_CONF_DIR"), "hbase-site.xml"));
		// config.addResource(new Path(System.getenv("HADOOP_CONF_DIR"), "core-site.xml"));
		try (Connection connection = ConnectionFactory.createConnection(config); Admin admin = connection.getAdmin()) {
		}
	}



	public static void createLog(Configuration config) throws Exception {
		try (Connection connection = ConnectionFactory.createConnection(config); Admin admin = connection.getAdmin();) {
			HTableDescriptor table = new HTableDescriptor(TableName.valueOf("LOG"));
			table.addFamily(new HColumnDescriptor("F").setCompressionType(Algorithm.GZ));
			Long userId = 2L;
			createOrOverwrite(admin, table);
			System.out.print("Created table LOG. ");
			Table t = connection.getTable(table.getTableName());
//			List<Put> puts = new ArrayList<Put>();
			for (int i = 0; i <= 1000; i++) {
				Long ts = System.currentTimeMillis();
				String rk = String.valueOf(userId) + ":" + String.valueOf(Long.MAX_VALUE - ts);
				System.out.println(rk);
				Put put = new Put(Bytes.toBytes(rk), ts);
				put.addColumn(Bytes.toBytes("F"), Bytes.toBytes("Q"), Bytes.toBytes(userId+"|"+ts+"|"+LogType.ZCDL+"|this is the " + i + " item"));
//				puts.add(put);
				t.put(put);
				Thread.sleep(1);
			}
//			t.put(puts);
			t.close();
		}
	}

	public static void searchLog(Configuration config) throws IOException {
		try (Connection connection = ConnectionFactory.createConnection(config);) {
			Table t = connection.getTable(TableName.valueOf("LOG"));
			Scan scan = new Scan();
			Filter filter = new RowFilter(CompareFilter.CompareOp.EQUAL, new RegexStringComparator("2:.+"));
			scan.setFilter(filter);
			ResultScanner rs = t.getScanner(scan);
			for (Result r : rs) {
				System.out.println(new String(r.value()));
			}
		}
	}
	public static void main(String... args) throws Exception {
		System.setProperty("hadoop.home.dir", "C:\\hadoop-common-2.2.0-bin-master\\");
		Configuration config = HBaseConfiguration.create();
		// createSchemaTables(config);
		// modifySchema(config);
		 createLog(config);
//		 searchLog3(config);
	}
	public static void searchLog2(Configuration config) throws IOException {
		try (Connection connection = ConnectionFactory.createConnection(config);) {
			Table t = connection.getTable(TableName.valueOf("LOG"));
			Filter filter = new PageFilter(15);
			int totalRows = 0;
			byte[] lastRow=null;
			while(true){
				Scan scan = new Scan();
				scan.setFilter(filter);
				if(lastRow!=null){
					byte[] startRow=Bytes.add(lastRow,new byte[]{});
					scan.setStartRow(startRow);
				}
				ResultScanner scanner = t.getScanner(scan);
				int localRows=0;
				Result r;
				scanner.next();
				while((r=scanner.next())!=null){
					System.out.println(localRows++ +": "+new String(r.value()));
					totalRows++;
					lastRow = r.getRow();
				}
				System.out.println("localRows: "+localRows);
				scanner.close();
				if(localRows == 0)break;
			}
			System.out.println("total rows: "+totalRows);
		}
	}
	public static void searchLog3(Configuration config) throws IOException {
		try (Connection connection = ConnectionFactory.createConnection(config);) {
			Table t = connection.getTable(TableName.valueOf("LOG"));
			Scan scan = new Scan();
//			scan.setStartRow(Bytes.toBytes("2:9223370584717845785"));
			FilterList fl = new FilterList();
			fl.addFilter(new RowFilter(CompareFilter.CompareOp.EQUAL, new RegexStringComparator("2:.+")));
			fl.addFilter(new PageFilter(10));
			
			scan.setFilter(fl);
			ResultScanner rs = t.getScanner(scan);
			for (Result r : rs) {
				System.out.println(new String(r.getRow())+"|"+new String(r.value()));
			}
		}
	}
}
