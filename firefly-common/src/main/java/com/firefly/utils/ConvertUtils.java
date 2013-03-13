package com.firefly.utils;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;

import com.firefly.utils.collection.IdentityHashMap;

abstract public class ConvertUtils {
	private static final IdentityHashMap<Class<?>, ParseValue> map = new IdentityHashMap<Class<?>, ParseValue>();
	private static final Map<String, ParseValue> map2 = new HashMap<String, ParseValue>();

	static {
		ParseValue p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return Integer.parseInt(value);
			}
		};
		map.put(int.class, p);
		map.put(Integer.class, p);
		map2.put("byte", p);
		map2.put("java.lang.Byte", p);
		
		p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return Long.parseLong(value);
			}
		};
		map.put(long.class, p);
		map.put(Long.class, p);
		map2.put("long", p);
		map2.put("java.lang.Long", p);
		
		p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return Double.parseDouble(value);
			}
		};
		map.put(double.class, p);
		map.put(Double.class, p);
		map2.put("double", p);
		map2.put("java.lang.Double", p);
		
		p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return Float.parseFloat(value);
			}
		};
		map.put(float.class, p);
		map.put(Float.class, p);
		map2.put("float", p);
		map2.put("java.lang.Float", p);
		
		p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return Boolean.parseBoolean(value);
			}
		};
		map.put(boolean.class, p);
		map.put(Boolean.class, p);
		map2.put("boolean", p);
		map2.put("java.lang.Boolean", p);
		
		p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return Short.parseShort(value);
			}
		};
		map.put(short.class, p);
		map.put(Short.class, p);
		map2.put("short", p);
		map2.put("java.lang.Short", p);
		
		p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return Byte.parseByte(value);
			}
		};
		map.put(byte.class, p);
		map.put(Byte.class, p);
		map2.put("byte", p);
		map2.put("java.lang.Byte", p);
		
		p = new ParseValue() {
			@Override
			public Object parse(String value) {
				return value;
			}
		};
		map.put(String.class, p);
		map2.put("java.lang.String", p);
	}

	interface ParseValue {
		Object parse(String value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T convert(String value, Class<T> c) {
		Object ret = null;
		ParseValue p = c == null ? null : map.get(c);
		if (p != null)
			ret = p.parse(value);
		else {
			if (VerifyUtils.isInteger(value)) {
				ret = Integer.parseInt(value);
			} else if (VerifyUtils.isLong(value)) {
				ret = Long.parseLong(value);
			} else if (VerifyUtils.isDouble(value)) {
				ret = Double.parseDouble(value);
			} else if (VerifyUtils.isFloat(value)) {
				ret = Float.parseFloat(value);
			} else
				ret = value;
		}
		return (T) ret;
	}

	@SuppressWarnings("unchecked")
	public static <T> T convert(String value, String argsType) {
		Object ret = null;
		ParseValue p = argsType == null ? null : map2.get(argsType);
		if (p != null)
			ret = p.parse(value);
		else {
			if (VerifyUtils.isInteger(value)) {
				ret = Integer.parseInt(value);
			} else if (VerifyUtils.isLong(value)) {
				ret = Long.parseLong(value);
			} else if (VerifyUtils.isDouble(value)) {
				ret = Double.parseDouble(value);
			} else if (VerifyUtils.isFloat(value)) {
				ret = Float.parseFloat(value);
			} else
				ret = value;
		}
		return (T) ret;
	}

	/**
	 * 把集合转换为指定类型的数组
	 * 
	 * @param collection
	 * @param type
	 * @return
	 */
	public static Object convert(Collection<?> collection, Class<?> arrayType) {
		int size = collection.size();
		// Allocate a new Array
		Iterator<?> iterator = collection.iterator();
		Class<?> componentType = null;

		if (arrayType == null) {
			componentType = Object.class;
		} else {
			if (!arrayType.isArray())
				throw new IllegalArgumentException("type is not a array");

			componentType = arrayType.getComponentType();
		}
		Object newArray = Array.newInstance(componentType, size);

		// Convert and set each element in the new Array
		for (int i = 0; i < size; i++) {
			Object element = iterator.next();
			try {
				ReflectUtils.arraySet(newArray, i, element);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		return newArray;
	}

	/**
	 * 根据类型自动返回一个集合
	 * 
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Collection getCollectionObj(Class<?> clazz) {
		if (clazz.isInterface()) {
			if (clazz.isAssignableFrom(List.class))
				return new ArrayList();
			else if (clazz.isAssignableFrom(Set.class))
				return new HashSet();
			else if (clazz.isAssignableFrom(Queue.class))
				return new ArrayDeque();
			else if (clazz.isAssignableFrom(SortedSet.class))
				return new TreeSet();
			else if (clazz.isAssignableFrom(BlockingQueue.class))
				return new LinkedBlockingDeque();
			else
				return null;
		} else {
			Collection collection = null;
			try {
				collection = (Collection) clazz.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return collection;
		}
	}

	@SuppressWarnings("rawtypes")
	public static Map getMapObj(Class<?> clazz) {
		if (clazz.isInterface()) {
			if (clazz.isAssignableFrom(Map.class))
				return new HashMap();
			else if (clazz.isAssignableFrom(ConcurrentMap.class))
				return new ConcurrentHashMap();
			else if (clazz.isAssignableFrom(SortedMap.class))
				return new TreeMap();
			else if (clazz.isAssignableFrom(NavigableMap.class))
				return new TreeMap();
			else if (clazz.isAssignableFrom(ConcurrentNavigableMap.class))
				return new ConcurrentSkipListMap();
			else
				return null;
		} else {
			Map map = null;
			try {
				map = (Map) clazz.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return map;
		}
	}

	public static <T> Enumeration<T> enumeration(Collection<T> col) {
		final Iterator<T> it = col.iterator();
		return new Enumeration<T>() {
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			public T nextElement() {
				return it.next();
			}
		};
	}
}
