package com.firefly.utils;

import com.firefly.utils.collection.IdentityHashMap;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;

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
	public static <T> T convert(String value, T defaultValue) {
		return convert(value, (Class<T>) defaultValue.getClass(), defaultValue);
	}

	public static <T> T convert(String value, Class<T> c, T defaultValue) {
		try {
			if (!StringUtils.hasText(value)) {
				return defaultValue;
			}
			T ret = convert(value, c);
			return ret != null ? ret : defaultValue;
		} catch (Throwable t) {
			return defaultValue;
		}
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
			} else {
				ret = value;
			}
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
			} else {
				ret = value;
			}
		}
		return (T) ret;
	}

	/**
	 * Returns an array object, this method converts a collection object to an
	 * array object through the specified element type of the array.
	 * 
	 * @param collection
	 *            The collection that needs be converted
	 * @param arrayType
	 *            The element type of an array
	 * @return a array object and the element is the parameter specified type.
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
				System.err.println("set element to array exception, " + e.getMessage());
			}
		}

		return newArray;
	}

	/**
	 * Returns a collection object instance by class
	 * 
	 * @param clazz
	 *            The class object of a collection
	 * @return A collection object instance
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> getCollectionObj(Class<?> clazz) {
		if (clazz.isInterface()) {
			if (clazz.isAssignableFrom(List.class))
				return new ArrayList<Object>();
			else if (clazz.isAssignableFrom(Set.class))
				return new HashSet<Object>();
			else if (clazz.isAssignableFrom(Queue.class))
				return new ArrayDeque<Object>();
			else if (clazz.isAssignableFrom(SortedSet.class))
				return new TreeSet<Object>();
			else if (clazz.isAssignableFrom(BlockingQueue.class))
				return new LinkedBlockingDeque<Object>();
			else
				return null;
		} else {
			Collection<Object> collection = null;
			try {
				collection = (Collection<Object>) clazz.newInstance();
			} catch (Exception e) {
				System.err.println("new collection instance exception, " + e.getMessage());
			}
			return collection;
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<Object, Object> getMapObj(Class<?> clazz) {
		if (clazz.isInterface()) {
			if (clazz.isAssignableFrom(Map.class))
				return new HashMap<Object, Object>();
			else if (clazz.isAssignableFrom(ConcurrentMap.class))
				return new ConcurrentHashMap<Object, Object>();
			else if (clazz.isAssignableFrom(SortedMap.class))
				return new TreeMap<Object, Object>();
			else if (clazz.isAssignableFrom(NavigableMap.class))
				return new TreeMap<Object, Object>();
			else if (clazz.isAssignableFrom(ConcurrentNavigableMap.class))
				return new ConcurrentSkipListMap<Object, Object>();
			else
				return null;
		} else {
			Map<Object, Object> map = null;
			try {
				map = (Map<Object, Object>) clazz.newInstance();
			} catch (Exception e) {
				System.err.println("new map instance exception, " + e.getMessage());
			}
			return map;
		}
	}

	public static <T> Enumeration<T> enumeration(Collection<T> col) {
		final Iterator<T> it = col.iterator();
		return new Enumeration<T>() {
			@Override
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public T nextElement() {
				return it.next();
			}
		};
	}

}
