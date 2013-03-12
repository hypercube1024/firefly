package com.firefly.template.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.firefly.template.Config;
import com.firefly.utils.ReflectUtils.ProxyMethod;


public class ObjectMetaInfoCache {
	private Map<String, ProxyMethod> map = new ConcurrentHashMap<String, ProxyMethod>();

	public ProxyMethod get(Class<?> clazz, String propertyName) {
		return map.get(clazz.getName() + "#" + propertyName);
	}

	public void put(Class<?> clazz, String propertyName, ProxyMethod method) {
		try {
			map.put(clazz.getName() + "#" + propertyName, method);
		} catch (Throwable e) {
			Config.LOG.error("put proxy error", e);
		}
	}

}
