package cn.lzhp.tools.service.util;

import java.util.Date;

import com.google.common.base.Throwables;

public class LogHelper {

	private LogHelper() {
		throw new IllegalStateException("LogHelper class");
	}

	public static void trace(String msg) {
		log(msg);
	}

	public static void trace(String msg, Throwable t) {
		log(msg, t);
	}

	public static void debug(String msg) {
		log(msg);
	}

	public static void debug(String msg, Throwable t) {
		log(msg, t);
	}

	public static void info(String msg) {
		log(msg);

	}

	public static void info(String msg, Throwable t) {
		log(msg, t);
	}

	public static void warn(String msg) {
		log(msg);
	}

	public static void warn(String msg, Throwable t) {
		log(msg, t);
	}

	public static void error(String msg) {
		log(msg);
	}

	public static void error(String msg, Throwable t) {
		log(msg, t);
	}

	private static String getAllMessage(String msg) {
		return String.format("thread name:[%s], time:[%tc], %s", Thread.currentThread().getName(), new Date(), msg);
	}

	private static String getAllMessage(String msg, Throwable t) {
		return String.format("thread name:[%s], time:[%tc], %s \r%n source:%s", Thread.currentThread().getName(),
				new Date(), msg, Throwables.getStackTraceAsString(t));
	}

	private static void log(String msg) {
		System.out.println(getAllMessage(msg));
	}

	private static void log(String msg, Throwable t) {
		System.out.println(getAllMessage(msg, t));
	}
}
