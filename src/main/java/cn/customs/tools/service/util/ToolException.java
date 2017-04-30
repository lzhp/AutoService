package cn.customs.tools.service.util;

public class ToolException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ToolException(){
		super();
	}
	
	public ToolException(String msg){
		super(msg);
	}

	public ToolException(String msg, Throwable t){
		super(msg,t);
	}
}
