package com.firefly.utils.collection;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 
 * @author Pengtao Qiu
 *
 * @param <T> the type of mapped node
 */
public class ConsistentHash<T> {

	public interface HashFunction {
		int hash(Object o);

		int hashWithVirtualNodeIndex(Object o, int index);
	}

	private HashFunction hashFunction = new HashFunction() {
		@Override
		public int hash(Object o) {
			return o.hashCode();
		}

		@Override
		public int hashWithVirtualNodeIndex(Object o, int index) {
			return hash(o.toString() + "_" + index); // need improve virtual node algorithm
		}
	};
	private int numberOfReplicas = 4;
	private NavigableMap<Integer, T> circle = new ConcurrentSkipListMap<Integer, T>();

	public ConsistentHash() {

	}
	
	public ConsistentHash(int numberOfReplicas) {
		this(null, numberOfReplicas, null);
	}

	public ConsistentHash(HashFunction hashFunction) {
		this(hashFunction, 0, null);
	}

	public ConsistentHash(HashFunction hashFunction, int numberOfReplicas, NavigableMap<Integer, T> circle) {
		this(hashFunction, numberOfReplicas, circle, null);
	}

	public ConsistentHash(HashFunction hashFunction, int numberOfReplicas, NavigableMap<Integer, T> circle, Collection<T> nodes) {
		if(hashFunction != null)
			this.hashFunction = hashFunction;
		
		if (numberOfReplicas > 0)
			this.numberOfReplicas = numberOfReplicas;

		if (circle != null)
			this.circle = circle;

		if (nodes != null) {
			for (T node : nodes) {
				add(node);
			}
		}
	}

	public void add(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.put(hashFunction.hashWithVirtualNodeIndex(node, i), node);
		}
	}

	public void remove(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.remove(hashFunction.hashWithVirtualNodeIndex(node, i));
		}
	}

	public T get(Object key) {
		if (circle.isEmpty())
			return null;

		int hash = hashFunction.hash(key);
		T t = circle.get(key);
		if (t != null)
			return t;

		Map.Entry<Integer, T> entry = circle.higherEntry(hash);
		return entry == null ? circle.firstEntry().getValue() : entry.getValue();
	}

}