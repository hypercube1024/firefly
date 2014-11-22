package com.firefly.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The ConcurrentLinkedHashMap is the reentrant version of LinkedHashMap, which is the thread safe collection.
 * It uses many partitions to mitigate parallel conflict.
 * As same as the LinkedHashMap, you can choose LRU or FIFO arithmetic to eliminate entry of the map.
 * @author Pengtao Qiu
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentLinkedHashMap<K, V> implements Map<K,V> {
	
	/**
	 * The callback interface, when the some events(get, put, remove, eliminate) occur, the map will call it. 
	 * @author qiupengtao
	 *
	 * @param <K> the type of keys maintained by this map
	 * @param <V> the type of mapped values
	 */
	public interface MapEventListener<K, V> {
		boolean onEliminateEntry(K key, V value);
		V onGetEntry(K key, V value);
		V onPutEntry(K key, V value, V previousValue);
		V onRemoveEntry(K key, V value);
	}
	
	/**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    /**
	 * The default concurrency level for this table, used when not otherwise
	 * specified in a constructor.
	 */
	static final int DEFAULT_CONCURRENCY_LEVEL = 16;
	
	/**
	 * Mask value for indexing into segments. The upper bits of a key's hash
	 * code are used to choose the segment.
	 */
	private final int segmentMask;

	/**
	 * Shift value for indexing within segments.
	 */
	private final int segmentShift;
	private final int concurrencyLevel;
	private final LinkedHashMapSegment<K, V>[] segments;
	private final MapEventListener<K, V> mapEventListener;
	
	/**
	 * 
	 * @param accessOrder the ordering mode - <tt>true</tt> for
     *         access-order, <tt>false</tt> for insertion-order
	 * @param maxEntries 
	 * 			map's the biggest capacity, it isn't accurate, 
	 * 			the actual limit of capacity depends on the entry's number of every segment.
	 * 			For example, if you set the max entries is 64, and the concurrency level is 16,
	 * 			and then every segment's max entries is 64/16 = 4. 
	 * @param mapEventListener the callback method of map's operations
	 */
	public ConcurrentLinkedHashMap(boolean accessOrder, 
 			int maxEntries,
 			MapEventListener<K, V> mapEventListener) {
		this(accessOrder, maxEntries, mapEventListener, DEFAULT_CONCURRENCY_LEVEL);
	}
	
	/**
	 * 
	 * @param accessOrder accessOrder the ordering mode - <tt>true</tt> for
     *         access-order, <tt>false</tt> for insertion-order
	 * @param maxEntries 
	 * 			map's the biggest capacity, it isn't accurate, 
	 * 			the actual limit of capacity depends on the entry's number of every segment.
	 * 			For example, if you set the max entries is 64, and the concurrency level is 16,
	 * 			and then every segment's max entries is 64/16 = 4.
	 * @param mapEventListener the callback method of map's operations
	 * @param concurrencyLevel the number of segment, default is 16
	 */
	public ConcurrentLinkedHashMap(boolean accessOrder, 
 			int maxEntries,
 			MapEventListener<K, V> mapEventListener,
 			int concurrencyLevel) {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, accessOrder, maxEntries, mapEventListener, concurrencyLevel);
	}
	
	/**
	 * 
	 * @param initialCapacity map initial capacity in every segment
	 * @param loadFactor the load factor decide the map increase to what degree have to expand, default value is 0.75f
	 * @param accessOrder the ordering mode - <tt>true</tt> for
     *         access-order, <tt>false</tt> for insertion-order
	 * @param maxEntries 
	 * 			map's the biggest capacity, it isn't accurate, 
	 * 			the actual limit of capacity depends on the entry's number of every segment.
	 * 			For example, if you set the max entries is 64, and the concurrency level is 16,
	 * 			and then every segment's max entries is 64/16 = 4.
	 * @param mapEventListener the callback method of map's operations
	 * @param concurrencyLevel the number of segment, default is 16
	 */
	@SuppressWarnings("unchecked")
	public ConcurrentLinkedHashMap(int initialCapacity,
 			float loadFactor,
 			boolean accessOrder, 
 			int maxEntries,
 			MapEventListener<K, V> mapEventListener,
 			int concurrencyLevel) {
		this.mapEventListener = mapEventListener;
		
		int cLevel = concurrencyLevel > 0 ? concurrencyLevel : DEFAULT_CONCURRENCY_LEVEL;
		// Find a power of 2 >= concurrencyLevel
        int level = 1;
        int sshift = 0;
        while (level < cLevel) {
            level <<= 1;
            sshift++;
        }
        segmentShift = 32 - sshift;
		segmentMask = level - 1;
        this.concurrencyLevel = level;
		
		segments = new LinkedHashMapSegment[this.concurrencyLevel];
		for (int i = 0; i < segments.length; i++) {
			LinkedHashMapSegment<K, V> segment = new LinkedHashMapSegment<K, V>(
					initialCapacity, 
					loadFactor, 
					accessOrder, 
					maxEntries <= this.concurrencyLevel ? 1 : (maxEntries / this.concurrencyLevel) , 
					mapEventListener);
			segments[i] = segment;
		}
		
	}
	
	static final class LinkedHashMapSegment<K, V> extends LinkedHashMap<K, V>{

		private static final long serialVersionUID = 3135160986591665845L;
		private final int maxEntries;
		private final MapEventListener<K, V> mapEventListener;
		final Lock lock = new ReentrantLock();
		
		public int getMaxEntries() {
			return maxEntries;
		}

		public LinkedHashMapSegment(int initialCapacity,
                         			float loadFactor,
                         			boolean accessOrder, 
                         			int maxEntries,
                         			MapEventListener<K, V> mapEventListener) {
			super(initialCapacity, loadFactor, accessOrder);
			this.maxEntries = maxEntries;
			this.mapEventListener = mapEventListener;
		}
		
		@Override
		protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
			lock.lock();
			try {
				if(size() > maxEntries) {
					return mapEventListener.onEliminateEntry(eldest.getKey(), eldest.getValue());
				}
		        return false;
			} finally {
				lock.unlock();
			}
	    }
		
	}
	
	/**
	 * Applies a supplemental hash function to a given hashCode, which defends
	 * against poor quality hash functions. This is critical because
	 * ConcurrentHashMap uses power-of-two length hash tables, that otherwise
	 * encounter collisions for hashCodes that do not differ in lower or upper
	 * bits.
	 */
	private static int hash(int h) {
		// Spread bits to regularize both segment and index locations,
		// using variant of single-word Wang/Jenkins hash.
		h += (h << 15) ^ 0xffffcd7d;
		h ^= (h >>> 10);
		h += (h << 3);
		h ^= (h >>> 6);
		h += (h << 2) + (h << 14);
		return h ^ (h >>> 16);
	}
	
	/**
	 * Returns the segment that should be used for key with given hash
	 * 
	 * @param hash the hash code for the key
	 * @return the segment
	 */
	private final LinkedHashMapSegment<K, V> segmentFor(int hash) {
		int h = hash(hash);
		return segments[(h >>> segmentShift) & segmentMask];
	}
	
	private void lockAllSegments() {
		for(LinkedHashMapSegment<K, V> seg : segments)
			seg.lock.lock();
	}
	
	private void unlockAllSegments() {
		for(LinkedHashMapSegment<K, V> seg : segments)
			seg.lock.unlock();
	}

	/**
	 * Gets the entry's total number.
	 * @return entry's number
	 */
	@Override
	public int size() {
		try {
			lockAllSegments();
			int size = 0;
			for(LinkedHashMapSegment<K, V> seg : segments) {
				size += seg.size();
			}
			return size;
		} finally {
			unlockAllSegments();
		}
	}

	/**
	 * Returns <tt>true</tt>, if the map doesn't contain any entry.
	 * @return <tt>true</tt> if this map contains no key-value mappings.
	 */
	@Override
	public boolean isEmpty() {
		try {
			lockAllSegments();
			for(LinkedHashMapSegment<K, V> seg : segments) {
				if(!seg.isEmpty())
					return false;
			}
			return true;
		} finally {
			unlockAllSegments();
		}
	}

	/**
	 * Returns <tt>true</tt>, if this map contains a mapping for the specified key.
	 * @return <tt>true</tt>, if this map contains a mapping for the specified key.
	 */
	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the specified value.
     * @return <tt>true</tt> if this map maps one or more keys to the specified value.
	 */
	@Override
	public boolean containsValue(Object value) {
		try {
			lockAllSegments();
			for(LinkedHashMapSegment<K, V> seg : segments) {
				if(seg.containsValue(value))
					return true;
			}
			return false;
		} finally {
			unlockAllSegments();
		}
	}


	/**
	 * Returns a value which the specified key is mapped.
	 * At the same time, this method will call the callback interface, the getting entry event will be triggered.
	 * 
	 * @param key the key whose associated value is to be returned
	 * @return the value which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		LinkedHashMapSegment<K, V> seg = segmentFor(key.hashCode());
		try {
			seg.lock.lock();
			return mapEventListener.onGetEntry((K)key, seg.get(key));
		} finally {
			seg.lock.unlock();
		}
		
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the key existed in the map, the entry will be replaced by specified entry.
	 * This method will call the putting entry event when the method is called.
	 * 
	 * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>,
     *         if the implementation supports <tt>null</tt> values.)
	 */
	@Override
	public V put(K key, V value) {
		LinkedHashMapSegment<K, V> seg = segmentFor(key.hashCode());
		try {
			seg.lock.lock();
			return mapEventListener.onPutEntry(key, value, seg.put(key, value));
		} finally {
			seg.lock.unlock();
		}
	}

	/**
	 * Remove a entry from this map. The removing event will be called.
	 * 
	 * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		LinkedHashMapSegment<K, V> seg = segmentFor(key.hashCode());
		try {
			seg.lock.lock();
			return mapEventListener.onRemoveEntry((K)key, seg.remove(key));
		} finally {
			seg.lock.unlock();
		}
	}

	/**
	 * Puts another map into this map.
	 * 
	 * @param m mappings to be stored in this map
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for(java.util.Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * clear all map's entries, but it dosen't trigger the remove callback method
	 */
	@Override
	public void clear() {
		try {
			lockAllSegments();
			for(LinkedHashMapSegment<K, V> seg : segments) {
				seg.clear();
			}
		} finally {
			unlockAllSegments();
		}
	}

	/**
	 * Gets the all key in this map.
	 * 
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public Set<K> keySet() {
		try {
			lockAllSegments();
			Set<K> set = new HashSet<K>();
			for(LinkedHashMapSegment<K, V> seg : segments) {
				set.addAll(seg.keySet());
			}
			return set;
		} finally {
			unlockAllSegments();
		}
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 * 
	 * @return a collection view of the values contained in this map
	 */
	@Override
	public Collection<V> values() {
		try {
			lockAllSegments();
			Collection<V> collection = new ArrayList<V>();
			for(LinkedHashMapSegment<K, V> seg : segments) {
				collection.addAll(seg.values());
			}
			return collection;
		} finally {
			unlockAllSegments();
		}
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 * 
	 * @return a set view of the mappings contained in this map
	 */
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		try {
			lockAllSegments();
			Set<java.util.Map.Entry<K, V>> set = new HashSet<java.util.Map.Entry<K, V>>();
			for(LinkedHashMapSegment<K, V> seg : segments) {
				set.addAll(seg.entrySet());
			}
			return set;
		} finally {
			unlockAllSegments();
		}
	}

	/**
	 * Returns a value which represents the number of map's partition
	 * @return a value which represents the partition's number.
	 */
	public int getConcurrencyLevel() {
		return concurrencyLevel;
	}
	
	/**
	 * Shift value for indexing within segments.
	 * @return shift value for indexing within segments.
	 */
	public int getSegmentShift() {
		return segmentShift;
	}

	/**
	 * Mask value for indexing into segments. The upper bits of a key's hash
	 * code are used to choose the segment.
	 *
	 * @return mask value for indexing into segments.
	 */
	public int getSegmentMask() {
		return segmentMask;
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			s.append("segment " + i + " -> " + segments[i].toString());
			s.append("\r\n");
		}
		return s.toString();
	}
	
}
