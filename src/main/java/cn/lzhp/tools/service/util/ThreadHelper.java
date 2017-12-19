package cn.lzhp.tools.service.util;

/**
 * @author lizhipeng .
 *
 */
public class ThreadHelper {

  private ThreadHelper() {
    throw new IllegalStateException("ThreadHelper class");
  }

  /**
   * 返回一个新的线程.
   * 
   * @param run run
   * @return Thread
   */
  public static Thread getNewThread(Runnable run) {
    return new Thread(run);
  }

}
