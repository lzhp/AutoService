package cn.lzhp.tools.service;

/**
 * @author lizhipeng
 * @version 1.0
 */
public interface IProcessor {

  /**
   * 处理接口.
   * @param workItem item
   * @param params stringParams
   */
  public void process(Object workItem, String params);

}
