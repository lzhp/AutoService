package cn.customs.tools.service;

/**
 * @author lizhipeng
 * @version 1.0
 */
public interface IReleaser {

	/**
	 * 
	 * @param workItem
	 * @param params
	 */
	public void release(Object workItem, String params);
	
	public void release(String lockID, String params);

}