package cn.lzhp.tools.service;

import java.util.List;

/**
 * @author lizhipeng
 * @version 1.0
 */
public interface IAccessor {
  /**.
   * 获取待处理的数据
   * 
   * @param lockString 系统提供的一个锁标记
   * @param params 获取数据时的参数
   * @return List
   */
  public List<Object> access(String lockString, String params);

}
