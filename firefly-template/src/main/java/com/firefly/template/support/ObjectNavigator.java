package com.firefly.template.support;

import java.util.AbstractCollection;
import java.util.Collection;
//import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.firefly.template.Config;
import com.firefly.template.Model;
import com.firefly.template.exception.ExpressionError;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.StringUtils;

public class ObjectNavigator {

	private ObjectNavigator() {}

	private class ArrayCollection<E> extends AbstractCollection<E> {

		private Object obj;
		private ArrayProxy arrayProxy;
		private int size;
		private Iterator<E> iterator;

		public ArrayCollection(Object array) {
			obj = array;
			try {
				arrayProxy = ReflectUtils.getArrayProxy(array.getClass());
				size = arrayProxy.size(array);
			} catch (Throwable e) {
				Config.LOG.error("get array proxy error", e);
			}
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
				return (E) arrayProxy.get(obj, i++);
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
		return (Collection<?>) (ret instanceof Collection ? ret : new ArrayCollection(ret));
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
	
	@SuppressWarnings("unchecked")
	public Comparable<Object> getComparable(Model model, String el) {
		Object ret = find(model, el);
		return (Comparable<Object>)ret;
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
		Object ret = null;
		try {
			ret = ReflectUtils.arrayGet(obj, index);
		} catch (Throwable e) {
			Config.LOG.error("get array value error", e);
		}
		return ret;
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

			if (isStringKey(keyEl)) {
				if (obj instanceof Map)
					ret = ((Map<?, ?>) obj).get(keyEl.substring(1, keyEl.length() - 1));
			} else { // list or array
				int index = Integer.parseInt(keyEl);
				if (obj instanceof List)
					ret = ((List<?>) obj).get(index);
				else if (obj instanceof Map)
					ret = ((Map<?, ?>) obj).get(index);
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
			
			if (isStringKey(keyEl)) {
				if (current instanceof Map)
					ret = ((Map<?, ?>) current).get(keyEl.substring(1, keyEl.length() - 1));
			} else { // list or array
				int index = Integer.parseInt(keyEl);
				if (current instanceof List)
					ret = ((List<?>) current).get(index);
				else if (current instanceof Map)
					ret = ((Map<?, ?>) current).get(index);
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

	private boolean isStringKey(String el) {
		char head = el.charAt(0);
		char tail = el.charAt(el.length() - 1);
		return ((head == '\'') && (tail == '\'')) || ((head == '"') && (tail == '"'));
	}

	private Object getObjectProperty(Object current, String propertyName) {
		Object ret = null;
		try {
			ret = ReflectUtils.get(current, propertyName);
		} catch (Throwable e) {
			Config.LOG.error("get property error", e);
		}
		return ret;
	}

}
