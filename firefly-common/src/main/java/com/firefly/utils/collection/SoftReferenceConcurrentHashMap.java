package com.firefly.utils.collection;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

public class SoftReferenceConcurrentHashMap<K, V> extends AbstractReferenceConcurrentHashMap<K, V> {

	private class ValueSoftReference extends SoftReference<V> {

		K key;

		public ValueSoftReference(K key, V value) {
			super(value, refQueue);
			this.key = key;
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void clearInvalidEntry(Reference<? extends V> ref) {
		map.remove(((ValueSoftReference) ref).key);
	}

	@Override
	protected Reference<V> createRefence(K key, V value) {
		return new ValueSoftReference(key, value);
	}

}
