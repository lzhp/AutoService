package cn.lzhp.tools.service.util;

/**
 * @author lizhipeng
 *
 */
public class ThreadHelper {
	

	/**
	 * 返回一个新的线程
	 * @param run
	 * @return
	 */
	public static Thread getNewThread(Runnable run){
		return new Thread(run);
	}

}
