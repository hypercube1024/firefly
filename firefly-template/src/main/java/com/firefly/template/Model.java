package com.firefly.template;

public interface Model {
	void put(String key, Object object);
	
	Object get(String key);
	
	void remove(String key);
	
	void clear();
}
