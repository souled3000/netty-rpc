package com.blackcrystalinfo.platform.util;

public class DateUtils {

	public static String secToTime(int time) {
		String timeStr = null;
		int hour = 0;
		int minute = 0;
		int second = 0;
		if (time > 0) {
			minute = time / 60;
			if (minute < 60) {
				second = time % 60;
			} else {
				hour = minute / 60;
				minute = minute % 60;
				second = time % 60;
			}
		}
		// timeStr = unitFormat(hour) + "时" + unitFormat(minute) + "分"
		// + unitFormat(second) + "秒";

		timeStr = combineStr(hour, minute, second);
		return timeStr;
	}

	public static String combineStr(int hour, int minute, int second) {
		StringBuilder sb = new StringBuilder();

		if (hour != 0) {
			sb.append(hour).append("小时");
		}

		if (0 != minute) {
			sb.append(minute).append("分钟");
		}

		if (0 != second) {
			sb.append(minute).append("秒");
		}

		return sb.toString();
	}

	public static String unitFormat(int i) {
		String retStr = null;
		if (i >= 0 && i < 10)
			retStr = "0" + Integer.toString(i);
		else
			retStr = "" + i;
		return retStr;
	}

	public static void main(String[] args) {
		System.out.println(secToTime(86400));
	}
}
