package com.firefly.utils.collection;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;
import java.util.*;
import java.io.Serializable;
import java.io.IOException;

public class ConcurrentLRUHashMap<K, V> extends AbstractMap<K, V> implements
		ConcurrentMap<K, V>, Serializable {

	/*
	 * The basic strategy is to subdivide the table among Segments, each of
	 * which itself is a concurrently readable hash table.
	 */

	/* ---------------- Constants -------------- */
	private static final long serialVersionUID = -5031526786765467550L;

	/**
	 * Segement默认最大数
	 */
	static final int DEFAULT_SEGEMENT_MAX_CAPACITY = 10000;

	/**
	 * The default load factor for this table, used when not otherwise specified
	 * in a constructor.
	 */
	static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * The default concurrency level for this table, used when not otherwise
	 * specified in a constructor.
	 */
	static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified by
	 * either of the constructors with arguments. MUST be a power of two <=
	 * 1<<30 to ensure that entries are indexable using ints.
	 */
	static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The maximum number of segments to allow; used to bound constructor
	 * arguments.
	 */
	static final int MAX_SEGMENTS = 1 << 16; // slightly conservative

	/**
	 * Number of unsynchronized retries in size and containsValue methods before
	 * resorting to locking. This is used to avoid unbounded retries if tables
	 * undergo continuous modification which would make it impossible to obtain
	 * an accurate result.
	 */
	static final int RETRIES_BEFORE_LOCK = 2;

	/* ---------------- Fields -------------- */

	/**
	 * Mask value for indexing into segments. The upper bits of a key's hash
	 * code are used to choose the segment.
	 */
	final int segmentMask;

	/**
	 * Shift value for indexing within segments.
	 */
	final int segmentShift;

	/**
	 * The segments, each of which is a specialized hash table
	 */
	final Segment<K, V>[] segments;

	transient Set<K> keySet;
	transient Set<Map.Entry<K, V>> entrySet;
	transient Collection<V> values;

	/* ---------------- Small Utilities -------------- */

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
	 * @param hash
	 *            the hash code for the key
	 * @return the segment
	 */
	final Segment<K, V> segmentFor(int hash) {
		return segments[(hash >>> segmentShift) & segmentMask];
	}

	/* ---------------- Inner Classes -------------- */

	/**
	 * 修改原HashEntry，
	 */
	static final class HashEntry<K, V> {
		final K key;
		final int hash;
		volatile V value;

		/**
		 * hash链指针
		 */
		final HashEntry<K, V> next;

		/**
		 * 双向链表的下一个节点
		 */
		HashEntry<K, V> linkNext;

		/**
		 * 双向链表的上一个节点
		 */
		HashEntry<K, V> linkPrev;

		/**
		 * 死亡标记
		 */
		AtomicBoolean dead;

		HashEntry(K key, int hash, HashEntry<K, V> next, V value) {
			this.key = key;
			this.hash = hash;
			this.next = next;
			this.value = value;
			dead = new AtomicBoolean(false);
		}

		@SuppressWarnings("unchecked")
		static final <K, V> HashEntry<K, V>[] newArray(int i) {
			return new HashEntry[i];
		}
	}

	/**
	 * 基于原Segment修改，内部实现一个双向列表
	 */
	static final class Segment<K, V> extends ReentrantLock implements
			Serializable {
		/*
		 * Segments maintain a table of entry lists that are ALWAYS kept in a
		 * consistent state, so can be read without locking. Next fields of
		 * nodes are immutable (final). All list additions are performed at the
		 * front of each bin. This makes it easy to check changes, and also fast
		 * to traverse. When nodes would otherwise be changed, new nodes are
		 * created to replace them. This works well for hash tables since the
		 * bin lists tend to be short. (The average length is less than two for
		 * the default load factor threshold.)
		 * 
		 * Read operations can thus proceed without locking, but rely on
		 * selected uses of volatiles to ensure that completed write operations
		 * performed by other threads are noticed. For most purposes, the
		 * "count" field, tracking the number of elements, serves as that
		 * volatile variable ensuring visibility. This is convenient because
		 * this field needs to be read in many read operations anyway:
		 * 
		 * - All (unsynchronized) read operations must first read the "count"
		 * field, and should not look at table entries if it is 0.
		 * 
		 * - All (synchronized) write operations should write to the "count"
		 * field after structurally changing any bin. The operations must not
		 * take any action that could even momentarily cause a concurrent read
		 * operation to see inconsistent data. This is made easier by the nature
		 * of the read operations in Map. For example, no operation can reveal
		 * that the table has grown but the threshold has not yet been updated,
		 * so there are no atomicity requirements for this with respect to
		 * reads.
		 * 
		 * As a guide, all critical volatile reads and writes to the count field
		 * are marked in code comments.
		 */

		private static final long serialVersionUID = 2249069246763182397L;

		/**
		 * The number of elements in this segment's region.
		 */
		transient volatile int count;

		/**
		 * Number of updates that alter the size of the table. This is used
		 * during bulk-read methods to make sure they see a consistent snapshot:
		 * If modCounts change during a traversal of segments computing size or
		 * checking containsValue, then we might have an inconsistent view of
		 * state so (usually) must retry.
		 */
		transient int modCount;

		/**
		 * The table is rehashed when its size exceeds this threshold. (The
		 * value of this field is always <tt>(int)(capacity *
		 * loadFactor)</tt>.)
		 */
		transient int threshold;

		/**
		 * The per-segment table.
		 */
		transient volatile HashEntry<K, V>[] table;

		/**
		 * The load factor for the hash table. Even though this value is same
		 * for all segments, it is replicated to avoid needing links to outer
		 * object.
		 * 
		 * @serial
		 */
		final float loadFactor;

		/**
		 * 头节点
		 */
		transient final HashEntry<K, V> header;

		/**
		 * Segement最大容量
		 */
		final int maxCapacity;
		
		/**
		 * map回调
		 */
		transient LRUMapEventListener listener = new LRUMapEventListener() {
			@Override
			public void eliminated(Object key, Object value) {
			}

			@Override
			public Object getNull(Object key) {
				return null;
			}
		};

		Segment(int maxCapacity, float lf, LRUMapEventListener listener) {
			this.maxCapacity = maxCapacity;
			loadFactor = lf;
			setTable(HashEntry.<K, V> newArray(maxCapacity));
			header = new HashEntry<K, V>(null, -1, null, null);
			header.linkNext = header;
			header.linkPrev = header;
			
			if(listener != null)
				this.listener = listener;
		}

		@SuppressWarnings("unchecked")
		static final <K, V> Segment<K, V>[] newArray(int i) {
			return new Segment[i];
		}

		/**
		 * Sets table to new HashEntry array. Call only while holding lock or in
		 * constructor.
		 */
		void setTable(HashEntry<K, V>[] newTable) {
			threshold = (int) (newTable.length * loadFactor);
			table = newTable;
		}

		/**
		 * Returns properly casted first entry of bin for given hash.
		 */
		HashEntry<K, V> getFirst(int hash) {
			HashEntry<K, V>[] tab = table;
			return tab[hash & (tab.length - 1)];
		}

		/**
		 * Reads value field of an entry under lock. Called if value field ever
		 * appears to be null. This is possible only if a compiler happens to
		 * reorder a HashEntry initialization with its table assignment, which
		 * is legal under memory model but is not known to ever occur.
		 */
		@SuppressWarnings("unchecked")
		V readValueUnderLock(HashEntry<K, V> e) {
			lock();
			try {
				return e.value == null ? (V) listener.getNull(e.key) : e.value;
			} finally {
				unlock();
			}
		}

		/* Specialized implementations of map methods */

		@SuppressWarnings("unchecked")
		V get(Object key, int hash) {
			if (count != 0) { // read-volatile
				HashEntry<K, V> e = getFirst(hash);
				while (e != null) {
					if (e.hash == hash && key.equals(e.key)) {
						V v = e.value;
						// 将节点移动到头节点之前
						moveNodeToHeader(e);
						if (v != null)
							return v;
						return readValueUnderLock(e); // recheck
					}
					e = e.next;
				}
			}
			return (V) listener.getNull(key);
		}

		/**
		 * 将节点移动到头节点之前
		 * 
		 * @param entry
		 */
		void moveNodeToHeader(HashEntry<K, V> entry) {
			// 先移除，然后插入到头节点的前面
			lock();
			try {
				removeNode(entry);
				addBefore(entry, header);
			} finally {
				unlock();
			}
		}
		
		/**
		 * 非同步的将节点移动到头结点之前
		 * @param entry
		 */
		void noSyncMoveNodeToHeader(HashEntry<K, V> entry) {
			removeNode(entry);
			addBefore(entry, header);
		}

		/**
		 * 将第一个参数代表的节点插入到第二个参数代表的节点之前
		 * 
		 * @param newEntry
		 *            需要插入的节点
		 * @param entry
		 *            被插入的节点
		 */
		void addBefore(HashEntry<K, V> newEntry, HashEntry<K, V> entry) {
			newEntry.linkNext = entry;
			newEntry.linkPrev = entry.linkPrev;
			entry.linkPrev.linkNext = newEntry;
			entry.linkPrev = newEntry;
		}

		/**
		 * 从双向链中删除该Entry
		 * 
		 * @param entry
		 */
		void removeNode(HashEntry<K, V> entry) {
			entry.linkPrev.linkNext = entry.linkNext;
			entry.linkNext.linkPrev = entry.linkPrev;
		}

		boolean containsKey(Object key, int hash) {
			if (count != 0) { // read-volatile
				HashEntry<K, V> e = getFirst(hash);
				while (e != null) {
					if (e.hash == hash && key.equals(e.key)) {
						moveNodeToHeader(e);
						return true;
					}

					e = e.next;
				}
			}
			return false;
		}

		boolean containsValue(Object value) {
			if (count != 0) { // read-volatile
				HashEntry<K, V>[] tab = table;
				int len = tab.length;
				for (int i = 0; i < len; i++) {
					for (HashEntry<K, V> e = tab[i]; e != null; e = e.next) {
						V v = e.value;
						if (v == null) // recheck
							v = readValueUnderLock(e);
						if (value.equals(v)) {
							moveNodeToHeader(e);
							return true;
						}
					}
				}
			}
			return false;
		}

		boolean replace(K key, int hash, V oldValue, V newValue) {
			lock();
			try {
				HashEntry<K, V> e = getFirst(hash);
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				boolean replaced = false;
				if (e != null && oldValue.equals(e.value)) {
					replaced = true;
					e.value = newValue;
					// 移动到头部
					noSyncMoveNodeToHeader(e);
				}
				return replaced;
			} finally {
				unlock();
			}
		}

		V replace(K key, int hash, V newValue) {
			lock();
			try {
				HashEntry<K, V> e = getFirst(hash);
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				V oldValue = null;
				if (e != null) {
					oldValue = e.value;
					e.value = newValue;
					// 移动到头部
					noSyncMoveNodeToHeader(e);
				}
				return oldValue;
			} finally {
				unlock();
			}
		}

		V put(K key, int hash, V value, boolean onlyIfAbsent) {
			lock();
			try {
				int c = count;
				if (c++ > threshold) // ensure capacity
					rehash();
				HashEntry<K, V>[] tab = table;
				int index = hash & (tab.length - 1);
				HashEntry<K, V> first = tab[index];
				HashEntry<K, V> e = first;
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				V oldValue = null;
				if (e != null) {
					oldValue = e.value;
					if (!onlyIfAbsent) {
						e.value = value;
						// 移动到头部
						noSyncMoveNodeToHeader(e);
					}
				} else {
					oldValue = null;
					++modCount;
					HashEntry<K, V> newEntry = new HashEntry<K, V>(key, hash,
							first, value);
					tab[index] = newEntry;
					count = c; // write-volatile
					// 添加到双向链
					addBefore(newEntry, header);
					// 判断是否达到最大值
					removeEldestEntry();
				}
				return oldValue;
			} finally {
				unlock();
			}
		}

		void rehash() {
			HashEntry<K, V>[] oldTable = table;
			int oldCapacity = oldTable.length;
			if (oldCapacity >= MAXIMUM_CAPACITY)
				return;

			/*
			 * Reclassify nodes in each list to new Map. Because we are using
			 * power-of-two expansion, the elements from each bin must either
			 * stay at same index, or move with a power of two offset. We
			 * eliminate unnecessary node creation by catching cases where old
			 * nodes can be reused because their next fields won't change.
			 * Statistically, at the default threshold, only about one-sixth of
			 * them need cloning when a table doubles. The nodes they replace
			 * will be garbage collectable as soon as they are no longer
			 * referenced by any reader thread that may be in the midst of
			 * traversing table right now.
			 */

			HashEntry<K, V>[] newTable = HashEntry.newArray(oldCapacity << 1);
			threshold = (int) (newTable.length * loadFactor);
			int sizeMask = newTable.length - 1;
			for (int i = 0; i < oldCapacity; i++) {
				// We need to guarantee that any existing reads of old Map can
				// proceed. So we cannot yet null out each bin.
				HashEntry<K, V> e = oldTable[i];

				if (e != null) {
					HashEntry<K, V> next = e.next;
					int idx = e.hash & sizeMask;

					// Single node on list
					if (next == null)
						newTable[idx] = e;

					else {
						// Reuse trailing consecutive sequence at same slot
						HashEntry<K, V> lastRun = e;
						int lastIdx = idx;
						for (HashEntry<K, V> last = next; last != null; last = last.next) {
							int k = last.hash & sizeMask;
							if (k != lastIdx) {
								lastIdx = k;
								lastRun = last;
							}
						}
						newTable[lastIdx] = lastRun;

						// Clone all remaining nodes
						for (HashEntry<K, V> p = e; p != lastRun; p = p.next) {
							int k = p.hash & sizeMask;
							HashEntry<K, V> n = newTable[k];
							HashEntry<K, V> newEntry = new HashEntry<K, V>(
									p.key, p.hash, n, p.value);

							newEntry.linkNext = p.linkNext;
							newEntry.linkPrev = p.linkPrev;
							newTable[k] = newEntry;
						}
					}
				}
			}
			table = newTable;
		}

		/**
		 * Remove; match on key only if value null, else match both.
		 */
		V remove(Object key, int hash, Object value) {
			lock();
			try {
				int c = count - 1;
				HashEntry<K, V>[] tab = table;
				int index = hash & (tab.length - 1);
				HashEntry<K, V> first = tab[index];
				HashEntry<K, V> e = first;
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;

				V oldValue = null;
				if (e != null) {
					V v = e.value;
					if (value == null || value.equals(v)) {
						oldValue = v;
						// All entries following removed node can stay
						// in list, but all preceding ones need to be
						// cloned.
						++modCount;
						HashEntry<K, V> newFirst = e.next;
						for (HashEntry<K, V> p = first; p != e; p = p.next) {
							newFirst = new HashEntry<K, V>(p.key, p.hash,
									newFirst, p.value);
							newFirst.linkNext = p.linkNext;
							newFirst.linkPrev = p.linkPrev;
						}
						tab[index] = newFirst;
						count = c; // write-volatile
						// 移除节点
						removeNode(e);
					}
				}
				return oldValue;
			} finally {
				unlock();
			}
		}

		/**
		 * 移除最旧元素
		 */
		void removeEldestEntry() {
			if (count > this.maxCapacity) {
				HashEntry<K, V> eldest = header.linkNext;
				listener.eliminated(eldest.key, eldest.value);
				remove(eldest.key, eldest.hash, null);
			}
		}

		void clear() {
			if (count != 0) {
				lock();
				try {
					HashEntry<K, V>[] tab = table;
					for (int i = 0; i < tab.length; i++)
						tab[i] = null;
					++modCount;
					count = 0; // write-volatile
				} finally {
					unlock();
				}
			}
		}
	}

	/**
	 * 使用指定参数，创建一个ConcurrentLRUHashMap
	 * 
	 * @param segementCapacity
	 *            Segement最大容量
	 * @param loadFactor
	 *            加载因子
	 * @param concurrencyLevel
	 *            并发级别
	 * @param listener
	 * 			map事件回调
	 */
	public ConcurrentLRUHashMap(int segementCapacity, float loadFactor,
			int concurrencyLevel, LRUMapEventListener listener) {
		if (!(loadFactor > 0) || segementCapacity < 0 || concurrencyLevel <= 0)
			throw new IllegalArgumentException();

		if (concurrencyLevel > MAX_SEGMENTS)
			concurrencyLevel = MAX_SEGMENTS;

		// Find power-of-two sizes best matching arguments
		int sshift = 0;
		int ssize = 1;
		while (ssize < concurrencyLevel) {
			++sshift;
			ssize <<= 1;
		}
		segmentShift = 32 - sshift;
		segmentMask = ssize - 1;
		this.segments = Segment.newArray(ssize);

		for (int i = 0; i < this.segments.length; ++i)
			this.segments[i] = new Segment<K, V>(segementCapacity, loadFactor, listener);
	}

	/**
	 * 使用指定参数，创建一个ConcurrentLRUHashMap
	 * 
	 * @param segementCapacity
	 *            Segement最大容量
	 * @param loadFactor
	 *            加载因子
	 */
	public ConcurrentLRUHashMap(int segementCapacity, float loadFactor) {
		this(segementCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, null);
	}

	/**
	 * 使用指定参数，创建一个ConcurrentLRUHashMap
	 * 
	 * @param segementCapacity
	 *            Segement最大容量
	 */
	public ConcurrentLRUHashMap(int segementCapacity) {
		this(segementCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, null);
	}

	/**
	 * 使用默认参数，创建一个ConcurrentLRUHashMap，存放元素最大数默认为16W， 加载因子为0.75，并发级别16
	 */
	public ConcurrentLRUHashMap() {
		this(DEFAULT_SEGEMENT_MAX_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, null);
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 * 
	 * @return <tt>true</tt> if this map contains no key-value mappings
	 */
	public boolean isEmpty() {
		final Segment<K, V>[] segments = this.segments;
		/*
		 * We keep track of per-segment modCounts to avoid ABA problems in which
		 * an element in one segment was added and in another removed during
		 * traversal, in which case the table was never actually empty at any
		 * point. Note the similar use of modCounts in the size() and
		 * containsValue() methods, which are the only other methods also
		 * susceptible to ABA problems.
		 */
		int[] mc = new int[segments.length];
		int mcsum = 0;
		for (int i = 0; i < segments.length; ++i) {
			if (segments[i].count != 0)
				return false;
			else
				mcsum += mc[i] = segments[i].modCount;
		}
		// If mcsum happens to be zero, then we know we got a snapshot
		// before any modifications at all were made. This is
		// probably common enough to bother tracking.
		if (mcsum != 0) {
			for (int i = 0; i < segments.length; ++i) {
				if (segments[i].count != 0 || mc[i] != segments[i].modCount)
					return false;
			}
		}
		return true;
	}

	/**
	 * Returns the number of key-value mappings in this map. If the map contains
	 * more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 * 
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		final Segment<K, V>[] segments = this.segments;
		long sum = 0;
		long check = 0;
		int[] mc = new int[segments.length];
		// Try a few times to get accurate count. On failure due to
		// continuous async changes in table, resort to locking.
		for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
			check = 0;
			sum = 0;
			int mcsum = 0;
			for (int i = 0; i < segments.length; ++i) {
				sum += segments[i].count;
				mcsum += mc[i] = segments[i].modCount;
			}
			if (mcsum != 0) {
				for (int i = 0; i < segments.length; ++i) {
					check += segments[i].count;
					if (mc[i] != segments[i].modCount) {
						check = -1; // force retry
						break;
					}
				}
			}
			if (check == sum)
				break;
		}
		if (check != sum) { // Resort to locking all segments
			sum = 0;
			for (int i = 0; i < segments.length; ++i)
				segments[i].lock();
			for (int i = 0; i < segments.length; ++i)
				sum += segments[i].count;
			for (int i = 0; i < segments.length; ++i)
				segments[i].unlock();
		}
		if (sum > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		else
			return (int) sum;

	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null}
	 * if this map contains no mapping for the key.
	 * 
	 * <p>
	 * More formally, if this map contains a mapping from a key {@code k} to a
	 * value {@code v} such that {@code key.equals(k)}, then this method returns
	 * {@code v}; otherwise it returns {@code null}. (There can be at most one
	 * such mapping.)
	 * 
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	public V get(Object key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).get(key, hash);
	}

	/**
	 * Tests if the specified object is a key in this table.
	 * 
	 * @param key
	 *            possible key
	 * @return <tt>true</tt> if and only if the specified object is a key in
	 *         this table, as determined by the <tt>equals</tt> method;
	 *         <tt>false</tt> otherwise.
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	public boolean containsKey(Object key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).containsKey(key, hash);
	}

	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the specified
	 * value. Note: This method requires a full internal traversal of the hash
	 * table, and so is much slower than method <tt>containsKey</tt>.
	 * 
	 * @param value
	 *            value whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map maps one or more keys to the specified
	 *         value
	 * @throws NullPointerException
	 *             if the specified value is null
	 */
	public boolean containsValue(Object value) {
		if (value == null)
			throw new NullPointerException();

		// See explanation of modCount use above

		final Segment<K, V>[] segments = this.segments;
		int[] mc = new int[segments.length];

		// Try a few times without locking
		for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
			int mcsum = 0;
			for (int i = 0; i < segments.length; ++i) {
				mcsum += mc[i] = segments[i].modCount;
				if (segments[i].containsValue(value))
					return true;
			}
			boolean cleanSweep = true;
			if (mcsum != 0) {
				for (int i = 0; i < segments.length; ++i) {
					if (mc[i] != segments[i].modCount) {
						cleanSweep = false;
						break;
					}
				}
			}
			if (cleanSweep)
				return false;
		}
		// Resort to locking all segments
		for (int i = 0; i < segments.length; ++i)
			segments[i].lock();
		boolean found = false;
		try {
			for (int i = 0; i < segments.length; ++i) {
				if (segments[i].containsValue(value)) {
					found = true;
					break;
				}
			}
		} finally {
			for (int i = 0; i < segments.length; ++i)
				segments[i].unlock();
		}
		return found;
	}

	/**
	 * Legacy method testing if some key maps into the specified value in this
	 * table. This method is identical in functionality to
	 * {@link #containsValue}, and exists solely to ensure full compatibility
	 * with class {@link java.util.Hashtable}, which supported this method prior
	 * to introduction of the Java Collections framework.
	 * 
	 * @param value
	 *            a value to search for
	 * @return <tt>true</tt> if and only if some key maps to the <tt>value</tt>
	 *         argument in this table as determined by the <tt>equals</tt>
	 *         method; <tt>false</tt> otherwise
	 * @throws NullPointerException
	 *             if the specified value is null
	 */
	public boolean contains(Object value) {
		return containsValue(value);
	}

	/**
	 * Put一个键值，加Map锁
	 */
	public V put(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).put(key, hash, value, false);
	}

	/**
	 * Put一个键值，如果该Key不存在的话
	 */
	public V putIfAbsent(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).put(key, hash, value, true);
	}

	/**
	 * Copies all of the mappings from the specified map to this one. These
	 * mappings replace any mappings that this map had for any of the keys
	 * currently in the specified map.
	 * 
	 * @param m
	 *            mappings to be stored in this map
	 */
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	/**
	 * Removes the key (and its corresponding value) from this map. This method
	 * does nothing if the key is not in the map.
	 * 
	 * @param key
	 *            the key that needs to be removed
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
	 *         if there was no mapping for <tt>key</tt>
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	public V remove(Object key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).remove(key, hash, null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws NullPointerException
	 *             if the specified key is null
	 */
	public boolean remove(Object key, Object value) {
		int hash = hash(key.hashCode());
		if (value == null)
			return false;
		return segmentFor(hash).remove(key, hash, value) != null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws NullPointerException
	 *             if any of the arguments are null
	 */
	public boolean replace(K key, V oldValue, V newValue) {
		if (oldValue == null || newValue == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).replace(key, hash, oldValue, newValue);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return the previous value associated with the specified key, or
	 *         <tt>null</tt> if there was no mapping for the key
	 * @throws NullPointerException
	 *             if the specified key or value is null
	 */
	public V replace(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		return segmentFor(hash).replace(key, hash, value);
	}

	/**
	 * Removes all of the mappings from this map.
	 */
	public void clear() {
		for (int i = 0; i < segments.length; ++i)
			segments[i].clear();
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map. The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa. The set supports element removal, which removes the
	 * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations. It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 * 
	 * <p>
	 * The view's <tt>iterator</tt> is a "weakly consistent" iterator that will
	 * never throw {@link ConcurrentModificationException}, and guarantees to
	 * traverse elements as they existed upon construction of the iterator, and
	 * may (but is not guaranteed to) reflect any modifications subsequent to
	 * construction.
	 */
	public Set<K> keySet() {
		Set<K> ks = keySet;
		return (ks != null) ? ks : (keySet = new KeySet());
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 * The collection is backed by the map, so changes to the map are reflected
	 * in the collection, and vice-versa. The collection supports element
	 * removal, which removes the corresponding mapping from this map, via the
	 * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>, <tt>removeAll</tt>,
	 * <tt>retainAll</tt>, and <tt>clear</tt> operations. It does not support
	 * the <tt>add</tt> or <tt>addAll</tt> operations.
	 * 
	 * <p>
	 * The view's <tt>iterator</tt> is a "weakly consistent" iterator that will
	 * never throw {@link ConcurrentModificationException}, and guarantees to
	 * traverse elements as they existed upon construction of the iterator, and
	 * may (but is not guaranteed to) reflect any modifications subsequent to
	 * construction.
	 */
	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null) ? vs : (values = new Values());
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map. The set
	 * is backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa. The set supports element removal, which removes the
	 * corresponding mapping from the map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
	 * <tt>clear</tt> operations. It does not support the <tt>add</tt> or
	 * <tt>addAll</tt> operations.
	 * 
	 * <p>
	 * The view's <tt>iterator</tt> is a "weakly consistent" iterator that will
	 * never throw {@link ConcurrentModificationException}, and guarantees to
	 * traverse elements as they existed upon construction of the iterator, and
	 * may (but is not guaranteed to) reflect any modifications subsequent to
	 * construction.
	 */
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}

	/**
	 * Returns an enumeration of the keys in this table.
	 * 
	 * @return an enumeration of the keys in this table
	 * @see #keySet()
	 */
	public Enumeration<K> keys() {
		return new KeyIterator();
	}

	/**
	 * Returns an enumeration of the values in this table.
	 * 
	 * @return an enumeration of the values in this table
	 * @see #values()
	 */
	public Enumeration<V> elements() {
		return new ValueIterator();
	}

	/* ---------------- Iterator Support -------------- */

	abstract class HashIterator {
		int nextSegmentIndex;
		int nextTableIndex;
		HashEntry<K, V>[] currentTable;
		HashEntry<K, V> nextEntry;
		HashEntry<K, V> lastReturned;

		HashIterator() {
			nextSegmentIndex = segments.length - 1;
			nextTableIndex = -1;
			advance();
		}

		public boolean hasMoreElements() {
			return hasNext();
		}

		final void advance() {
			if (nextEntry != null && (nextEntry = nextEntry.next) != null)
				return;

			while (nextTableIndex >= 0) {
				if ((nextEntry = currentTable[nextTableIndex--]) != null)
					return;
			}

			while (nextSegmentIndex >= 0) {
				Segment<K, V> seg = segments[nextSegmentIndex--];
				if (seg.count != 0) {
					currentTable = seg.table;
					for (int j = currentTable.length - 1; j >= 0; --j) {
						if ((nextEntry = currentTable[j]) != null) {
							nextTableIndex = j - 1;
							return;
						}
					}
				}
			}
		}

		public boolean hasNext() {
			return nextEntry != null;
		}

		HashEntry<K, V> nextEntry() {
			if (nextEntry == null)
				throw new NoSuchElementException();
			lastReturned = nextEntry;
			advance();
			return lastReturned;
		}

		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			ConcurrentLRUHashMap.this.remove(lastReturned.key);
			lastReturned = null;
		}
	}

	final class KeyIterator extends HashIterator implements Iterator<K>,
			Enumeration<K> {
		public K next() {
			return super.nextEntry().key;
		}

		public K nextElement() {
			return super.nextEntry().key;
		}
	}

	final class ValueIterator extends HashIterator implements Iterator<V>,
			Enumeration<V> {
		public V next() {
			return super.nextEntry().value;
		}

		public V nextElement() {
			return super.nextEntry().value;
		}
	}

	/**
	 * Custom Entry class used by EntryIterator.next(), that relays setValue
	 * changes to the underlying map.
	 */
	final class WriteThroughEntry extends AbstractMap.SimpleEntry<K, V> {
		/**
 * 
 */
		private static final long serialVersionUID = -2545938966452012894L;

		WriteThroughEntry(K k, V v) {
			super(k, v);
		}

		/**
		 * Set our entry's value and write through to the map. The value to
		 * return is somewhat arbitrary here. Since a WriteThroughEntry does not
		 * necessarily track asynchronous changes, the most recent "previous"
		 * value could be different from what we return (or could even have been
		 * removed in which case the put will re-establish). We do not and
		 * cannot guarantee more.
		 */
		public V setValue(V value) {
			if (value == null)
				throw new NullPointerException();
			V v = super.setValue(value);
			ConcurrentLRUHashMap.this.put(getKey(), value);
			return v;
		}
	}

	final class EntryIterator extends HashIterator implements
			Iterator<Entry<K, V>> {
		public Map.Entry<K, V> next() {
			HashEntry<K, V> e = super.nextEntry();
			return new WriteThroughEntry(e.key, e.value);
		}
	}

	final class KeySet extends AbstractSet<K> {
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		public int size() {
			return ConcurrentLRUHashMap.this.size();
		}

		public boolean contains(Object o) {
			return ConcurrentLRUHashMap.this.containsKey(o);
		}

		public boolean remove(Object o) {
			return ConcurrentLRUHashMap.this.remove(o) != null;
		}

		public void clear() {
			ConcurrentLRUHashMap.this.clear();
		}
	}

	final class Values extends AbstractCollection<V> {
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		public int size() {
			return ConcurrentLRUHashMap.this.size();
		}

		public boolean contains(Object o) {
			return ConcurrentLRUHashMap.this.containsValue(o);
		}

		public void clear() {
			ConcurrentLRUHashMap.this.clear();
		}
	}

	final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			V v = ConcurrentLRUHashMap.this.get(e.getKey());
			return v != null && v.equals(e.getValue());
		}

		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			return ConcurrentLRUHashMap.this.remove(e.getKey(), e.getValue());
		}

		public int size() {
			return ConcurrentLRUHashMap.this.size();
		}

		public void clear() {
			ConcurrentLRUHashMap.this.clear();
		}
	}

	/* ---------------- Serialization Support -------------- */

	/**
	 * Save the state of the <tt>ConcurrentHashMap</tt> instance to a stream
	 * (i.e., serialize it).
	 * 
	 * @param s
	 *            the stream
	 * @serialData the key (Object) and value (Object) for each key-value
	 *             mapping, followed by a null pair. The key-value mappings are
	 *             emitted in no particular order.
	 */
	private void writeObject(java.io.ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();

		for (int k = 0; k < segments.length; ++k) {
			Segment<K, V> seg = segments[k];
			seg.lock();
			try {
				HashEntry<K, V>[] tab = seg.table;
				for (int i = 0; i < tab.length; ++i) {
					for (HashEntry<K, V> e = tab[i]; e != null; e = e.next) {
						s.writeObject(e.key);
						s.writeObject(e.value);
					}
				}
			} finally {
				seg.unlock();
			}
		}
		s.writeObject(null);
		s.writeObject(null);
	}

	/**
	 * Reconstitute the <tt>ConcurrentHashMap</tt> instance from a stream (i.e.,
	 * deserialize it).
	 * 
	 * @param s
	 *            the stream
	 */
	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream s) throws IOException,
			ClassNotFoundException {
		s.defaultReadObject();

		// Initialize each segment to be minimally sized, and let grow.
		for (int i = 0; i < segments.length; ++i) {
			segments[i].setTable(new HashEntry[1]);
		}

		// Read the keys and values, and put the mappings in the table
		for (;;) {
			K key = (K) s.readObject();
			V value = (V) s.readObject();
			if (key == null)
				break;
			put(key, value);
		}
	}
}
