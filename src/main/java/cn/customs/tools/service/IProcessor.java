package cn.customs.tools.service;

/**
 * @author lizhipeng
 * @version 1.0
 */
public interface IProcessor {

	/**
	 * 
	 * @param workItem
	 * @param params
	 */
	public void process(Object workItem, String params);

}