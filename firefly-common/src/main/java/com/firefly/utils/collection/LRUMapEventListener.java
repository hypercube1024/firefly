package com.firefly.utils.collection;

public interface LRUMapEventListener<K, V> {
	void eliminated(K key, V value);
	
	V getNull(K key);
}
