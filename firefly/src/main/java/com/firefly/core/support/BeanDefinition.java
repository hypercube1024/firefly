package com.firefly.core.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;



/**
 * Bean information, the id, className or interface name is used for map's key.
 */
public interface BeanDefinition {

	String getId();
	
	void setId(String id);

	String getClassName();

	void setClassName(String className);

	String[] getInterfaceNames();

	void setInterfaceNames(String[] names);
	
	Constructor<?> getConstructor();
	
	void setConstructor(Constructor<?> constructor);
	
	void setInitMethod(Method method);
	
	Method getInitMethod();
}
