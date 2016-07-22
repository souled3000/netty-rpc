package com.blackcrystalinfo.platform.powersocket.mobile;

import static com.blackcrystalinfo.platform.common.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.common.RespField.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.util.Bytes;

import com.blackcrystalinfo.platform.common.Constants;
import com.blackcrystalinfo.platform.common.DataHelper;
import com.blackcrystalinfo.platform.common.ErrorCode;
import com.blackcrystalinfo.platform.server.HandlerAdapter;
import com.blackcrystalinfo.platform.server.RpcRequest;

//@Controller("/mobile/log")
public class UserLogApi extends HandlerAdapter {

//	private Logger logger = LoggerFactory.getLogger(UserLogApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object, Object> ret = new HashMap<Object, Object>();
		ret.put(status, SYSERROR.toString());

		// 入参解析：cookie
		String startRow = req.getParameter("ts");

		String userId = req.getUserId();
		System.out.println(userId);
		List<String> logs = new ArrayList<String>();
		Connection connection = DataHelper.getHCon();
		try (Table t = connection.getTable(TableName.valueOf(Bytes.toBytes("LOG")));) {
			FilterList fl = new FilterList();
			Scan scan = new Scan();
			if (StringUtils.isNotBlank(startRow)) {
				StringBuilder rk = new StringBuilder();
				rk.append(userId);
				rk.append(":");
				rk.append(Long.MAX_VALUE-Long.valueOf(startRow));
				scan.setStartRow(Bytes.toBytes(rk.toString()));
				fl.addFilter(new PageFilter(Constants.LOGPAGESIZE+1));
			} else {
				fl.addFilter(new PageFilter(Constants.LOGPAGESIZE));
			}
			fl.addFilter(new RowFilter(CompareFilter.CompareOp.EQUAL, new RegexStringComparator(userId + "\\:.+")));
			scan.setFilter(fl);
			ResultScanner rs = t.getScanner(scan);
			for (Result r : rs) {
//				String rk = new String(r.getRow());
				logs.add(new String(r.value()));
			}
			rs.close();
		}
		ret.put("logs", logs);
		ret.put(status, ErrorCode.SUCCESS.toString());
		return ret;
	}

}
