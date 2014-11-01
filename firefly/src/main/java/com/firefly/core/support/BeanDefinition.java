package com.firefly.core.support;

import java.lang.reflect.Constructor;



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

//	Object getObject();
//
//	void setObject(Object object);
}
