package cn.customs.tools.service;

import java.util.List;

/**
 * @author lizhipeng
 * @version 1.0
 */
public interface IAccessor {

	/**
	 * 
	 * @param params
	 */
	public List<Object> access(String lockString, String params);

}