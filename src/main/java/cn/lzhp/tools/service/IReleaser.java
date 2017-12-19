package cn.lzhp.tools.service;

/**
 * @author lizhipeng
 * @version 1.0
 */
public interface IReleaser {

  /**
   * 释放接口.
   * 
   * @param workItem workitem 
   * @param releaserParams stringparam
   */
  public void release(Object workItem, String releaserParams);

  public void releaseAll(String lockId, String releaserParams);

}
