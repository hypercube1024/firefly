package com.firefly.utils.collection;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

abstract public class AbstractReferenceConcurrentHashMap<K, V> implements Map<K, V>, ConcurrentMap<K, V> {

	protected Map<K, Reference<V>> map = new ConcurrentHashMap<>();
	protected ReferenceQueue<V> refQueue = new ReferenceQueue<>();

	private void clearAllInvalidEntry() {
		Reference<? extends V> ref;
		while ((ref = refQueue.poll()) != null) {
			clearInvalidEntry(ref);
		}
	}

	abstract protected void clearInvalidEntry(Reference<? extends V> ref);

	abstract protected Reference<V> createRefence(K key, V value);

	@Override
	public int size() {
		clearAllInvalidEntry();
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		clearAllInvalidEntry();
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public V get(Object key) {
		Reference<V> ref = map.get(key);
		if (ref == null)
			return null;

		V value = ref.get();
		if (value == null) {
			clearInvalidEntry(ref);
			return null;
		} else {
			return value;
		}
	}

	@Override
	public V put(K key, V value) {
		clearAllInvalidEntry();
		Reference<V> ref = map.put(key, createRefence(key, value));
		return ref != null ? ref.get() : null;
	}

	@Override
	public V putIfAbsent(K key, V value) {
		clearAllInvalidEntry();
		Reference<V> ref = map.putIfAbsent(key, createRefence(key, value));
		return ref != null ? ref.get() : null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		clearAllInvalidEntry();
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V replace(K key, V value) {
		Reference<V> ref = map.replace(key, createRefence(key, value));
		return ref != null ? ref.get() : null;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return map.replace(key, createRefence(key, oldValue), createRefence(key, newValue));
	}

	@Override
	public V remove(Object key) {
		clearAllInvalidEntry();
		Reference<V> ref = map.remove(key);
		return ref != null ? ref.get() : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object key, Object value) {
		clearAllInvalidEntry();
		return map.remove(key, createRefence((K) key, (V) value));
	}

	@Override
	public void clear() {
		clearAllInvalidEntry();
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		Collection<V> collection = new LinkedList<>();
		for (Map.Entry<K, V> entry : entrySet()) {
			collection.add(entry.getValue());
		}
		return collection;
	}

	@Override
	public boolean containsValue(Object value) {
		for (V v : values()) {
			if (v != null && v.equals(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> set = new HashSet<>();
		for (Map.Entry<K, Reference<V>> entry : map.entrySet()) {
			Reference<V> ref = entry.getValue();
			V value = ref.get();
			if (value == null) {
				clearInvalidEntry(ref);
			} else {
				set.add(new MapEntryImpl(entry.getKey(), value));
			}
		}
		return set;
	}

	protected class MapEntryImpl implements Map.Entry<K, V> {

		K key;
		V value;

		public MapEntryImpl(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			this.value = value;
			return value;
		}

		@Override
		public String toString() {
			return "MapEntry [key=" + key + ", value=" + value + "]";
		}
	}

}
