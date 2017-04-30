package cn.customs.tools.service.util;

public class LogHelper {

	public static void trace(String msg) {
		System.out.println(msg);
	}

	public static void trace(String msg, Throwable t) {
		System.out.println(msg);
		t.printStackTrace();
	}
	
	public static void debug(String msg) {
		System.out.println(msg);
	}

	public static void debug(String msg, Throwable t) {
		System.out.println(msg);
		t.printStackTrace();
	}
	
	public static void info(String msg) {
		System.out.println(msg);

	}

	public static void info(String msg, Throwable t) {
		System.out.println(msg);
		t.printStackTrace();
	}
	
	public static void warn(String msg) {
		System.out.println(msg);
	}

	public static void warn(String msg, Throwable t) {
		System.out.println(msg);
		t.printStackTrace();
	}	
	
	public static void error(String msg) {
		System.out.println(msg);
	}

	public static void error(String msg, Throwable t) {
		System.out.println(msg);
		t.printStackTrace();
	}	
}
