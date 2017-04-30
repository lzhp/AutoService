package cn.customs.tools.service.util;

public class ReflectHelper {

	public static Object getClassInstance(String className) {
		Object result = null;
		try {
			Class<?> c = Class.forName(className);
			result = c.newInstance();
		} catch (InstantiationException | IllegalAccessException 
				| IllegalArgumentException | ClassNotFoundException
				| SecurityException e) {
			throw new ToolException("failure in create class:[" +className+"]",e);
		}
		return result;
	}
}
