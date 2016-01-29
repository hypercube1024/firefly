package com.firefly.utils.collection;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public interface ConcurrentAutomaticClearMap<K, V> extends Map<K, V> , ConcurrentMap<K, V> {
	
	public void clearAllInvalidEntry();
	
}
