package com.firefly.template.support;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.firefly.template.Config;
import com.firefly.template.Model;
import com.firefly.template.exception.ExpressionError;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ProxyMethod;
import com.firefly.utils.StringUtils;

public class ObjectNavigator {
	private ObjectMetaInfoCache cache;
	private IdentityHashMap<Class<?>, ArrayObj> map;

	private ObjectNavigator() {
		this.cache = new ObjectMetaInfoCache();
		this.map = new IdentityHashMap<Class<?>, ArrayObj>();

		map.put(long[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((long[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((long[]) obj).length;
			}
		});
		map.put(double[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((double[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((double[]) obj).length;
			}
		});
		map.put(int[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((int[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((int[]) obj).length;
			}
		});
		map.put(float[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((float[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((float[]) obj).length;
			}
		});
		map.put(char[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((char[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((char[]) obj).length;
			}
		});
		map.put(boolean[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((boolean[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((boolean[]) obj).length;
			}
		});
		map.put(short[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((short[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((short[]) obj).length;
			}
		});
		map.put(byte[].class, new ArrayObj() {
			@Override
			public Object get(Object obj, int index) {
				return ((byte[]) obj)[index];
			}

			@Override
			public int size(Object obj) {
				return ((byte[]) obj).length;
			}
		});
	}

	interface ArrayObj {
		Object get(Object obj, int index);

		int size(Object obj);
	}

	private class ArrayCollection<E> extends AbstractCollection<E> {

		private Object obj;
		private Object[] array;
		private ArrayObj arrayObj;
		private boolean isArrayObj;
		private int size;
		private Iterator<E> iterator;

		public ArrayCollection(Object array) {
			obj = array;
			ArrayObj a = map.get(array.getClass());
			isArrayObj = a != null;
			arrayObj = a;
			if (!isArrayObj) {
				this.array = (Object[]) array;
				size = this.array.length;
			} else
				size = a.size(array);
			iterator = new ArrayInterator();
		}

		private class ArrayInterator implements Iterator<E> {

			private int i = 0;

			@Override
			public boolean hasNext() {
				return i < size;
			}

			@SuppressWarnings("unchecked")
			@Override
			public E next() {
				return (E) (isArrayObj ? arrayObj.get(obj, i++) : array[i++]);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		}

		@Override
		public Iterator<E> iterator() {
			return iterator;
		}

		@Override
		public int size() {
			return size;
		}

	}

	private static class Holder {
		private static ObjectNavigator instance = new ObjectNavigator();
	}

	public static ObjectNavigator getInstance() {
		return Holder.instance;
	}

	@SuppressWarnings("rawtypes")
	public Collection<?> getCollection(Model model, String el) {
		Object ret = find(model, el);
		return (Collection<?>) (ret instanceof Collection ? ret
				: new ArrayCollection(ret));
	}

	public Integer getInteger(Model model, String el) {
		Object ret = find(model, el);
		return ret != null ? ((Number) ret).intValue() : 0;
	}

	public Float getFloat(Model model, String el) {
		Object ret = find(model, el);
		return ret != null ? ((Number) ret).floatValue() : 0F;
	}

	public Long getLong(Model model, String el) {
		Object ret = find(model, el);
		return ret != null ? ((Number) ret).longValue() : 0L;
	}

	public Double getDouble(Model model, String el) {
		Object ret = find(model, el);
		return ret != null ? ((Number) ret).doubleValue() : 0.0;
	}

	public Boolean getBoolean(Model model, String el) {
		Object ret = find(model, el);
		return ret != null ? (Boolean) ret : false;
	}

	public String getValue(Model model, String el) {
		Object ret = find(model, el);
		return ret != null ? String.valueOf(ret) : "";
	}

	public Object find(Model model, String el) {
		Object current = null;
		String[] elements = StringUtils.split(el, '.');
		if ((elements != null) && (elements.length > 0)) {
			current = getObject(model, elements[0]);
			if (current == null)
				return null;

			for (int i = 1; i < elements.length; i++) {
				current = getObject(current, elements[i]);
			}
		}
		return current;
	}

	private Object getArrayObject(Object obj, int index) {
		ArrayObj a = map.get(obj.getClass());
		if (a != null)
			return a.get(obj, index);
		else
			return ((Object[]) obj)[index];
	}

	private Object getObject(Object current, String el) {
		boolean root = current instanceof Model;
		String element = el.trim();
		int listOrMapPrefixIndex = element.indexOf('[');
		if (listOrMapPrefixIndex > 0) { // map or list or array
			int listOrMapSuffixIndex = element.indexOf(']', listOrMapPrefixIndex);

			String keyEl = element.substring(listOrMapPrefixIndex + 1, listOrMapSuffixIndex);
			String p = element.substring(0, listOrMapPrefixIndex);
			Object obj = root ? ((Model) current).get(p) : getObjectProperty(current, p);
			if(obj == null)
				return null;
			
			Object ret = null;

			if (isMapKey(keyEl)) { // map
				if ((obj instanceof Map))
					ret = ((Map<?, ?>) obj).get(keyEl.substring(1, keyEl.length() - 1));
			} else { // list or array
				int index = Integer.parseInt(keyEl);
				if ((obj instanceof List))
					ret = ((List<?>) obj).get(index);
				else
					ret = getArrayObject(obj, index);
			}
			
			if (listOrMapSuffixIndex != element.length() - 1) {
				if(element.charAt(listOrMapSuffixIndex + 1) != '[')
					throw new ExpressionError("list or map expression error: " + element);
				
				return getObject(ret, element.substring(listOrMapSuffixIndex + 1));
			}
			return ret;
		} else if (listOrMapPrefixIndex < 0) { // object
			return root ? ((Model) current).get(element) : getObjectProperty(current, element);
		} else if (listOrMapPrefixIndex == 0) { // map['foo']['bar']['hello']
			int listOrMapSuffixIndex = element.indexOf(']', listOrMapPrefixIndex);
			String keyEl = element.substring(listOrMapPrefixIndex + 1, listOrMapSuffixIndex);
			
			Object ret = null;
			
			if (isMapKey(keyEl)) { // map
				if ((current instanceof Map))
					ret = ((Map<?, ?>) current).get(keyEl.substring(1, keyEl.length() - 1));
			} else { // list or array
				int index = Integer.parseInt(keyEl);
				if ((current instanceof List))
					ret = ((List<?>) current).get(index);
				else
					ret = getArrayObject(current, index);
			}
			
			if (listOrMapSuffixIndex != element.length() - 1) {
				if(element.charAt(listOrMapSuffixIndex + 1) != '[')
					throw new ExpressionError("list or map expression error: " + element);
				
				return getObject(ret, element.substring(listOrMapSuffixIndex + 1));
			}
			return ret;
		} else {
			throw new ExpressionError("expression error: " + element);
		}
	}

	private boolean isMapKey(String el) {
		char head = el.charAt(0);
		char tail = el.charAt(el.length() - 1);
		return ((head == '\'') && (tail == '\'')) || ((head == '"') && (tail == '"'));
	}

	private Object getObjectProperty(Object current, String propertyName) {
		Class<?> clazz = current.getClass();
		ProxyMethod proxy = cache.get(clazz, propertyName);
		if (proxy == null) {
			try {
				proxy = ReflectUtils.getProxyMethod(ReflectUtils.getGetterMethod(clazz, propertyName));
				cache.put(clazz, propertyName, proxy);
			} catch (Throwable e) {
				Config.LOG.error("get proxy method error", e);
			}
		}
		return proxy.invoke(current);
	}

}
