package com.firefly.template.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ObjectMetaInfoCache {
	private Map<String, Method> map;

	public ObjectMetaInfoCache() {
		map = new ConcurrentHashMap<String, Method>();
	}

	public Method get(Class<?> clazz, String propertyName) {
		return map.get(clazz.getName() + "#" + propertyName);
	}

	public void put(Class<?> clazz, String propertyName, Method method) {
		map.put(clazz.getName() + "#" + propertyName, method);
	}

}
