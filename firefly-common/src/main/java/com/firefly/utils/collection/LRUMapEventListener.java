package com.firefly.utils.collection;

public interface LRUMapEventListener {
	void eliminated(Object key, Object value);
}
