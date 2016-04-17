package com.firefly.db;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.BeanProcessor;

import com.firefly.db.annotation.Column;
import com.firefly.db.annotation.Id;
import com.firefly.utils.StringUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class DefaultBeanProcessor extends BeanProcessor {

	private final static Log log = LogFactory.getInstance().getLog("firefly-system");

	/**
	 * Set a bean's primitive properties to these defaults when SQL NULL is
	 * returned. These are the same as the defaults that ResultSet get* methods
	 * return in the event of a NULL column.
	 */
	private static final Map<Class<?>, Object> primitiveDefaults = new HashMap<Class<?>, Object>();
	static {
		primitiveDefaults.put(Integer.TYPE, Integer.valueOf(0));
		primitiveDefaults.put(Short.TYPE, Short.valueOf((short) 0));
		primitiveDefaults.put(Byte.TYPE, Byte.valueOf((byte) 0));
		primitiveDefaults.put(Float.TYPE, Float.valueOf(0f));
		primitiveDefaults.put(Double.TYPE, Double.valueOf(0d));
		primitiveDefaults.put(Long.TYPE, Long.valueOf(0L));
		primitiveDefaults.put(Boolean.TYPE, Boolean.FALSE);
		primitiveDefaults.put(Character.TYPE, Character.valueOf((char) 0));
	}

	@Override
	public <T> T toBean(ResultSet rs, Class<T> type) throws SQLException {
		PropertyDescriptor[] props = this.propertyDescriptors(type);
		ResultSetMetaData rsmd = rs.getMetaData();
		int[] columnToProperty = this.mapColumnsToProperties(rsmd, props, type);
		return this.createBean(rs, type, props, columnToProperty);
	}

	@Override
	public <T> List<T> toBeanList(ResultSet rs, Class<T> type) throws SQLException {
		List<T> results = new ArrayList<T>();

		if (!rs.next()) {
			return results;
		}

		PropertyDescriptor[] props = this.propertyDescriptors(type);
		ResultSetMetaData rsmd = rs.getMetaData();
		int[] columnToProperty = this.mapColumnsToProperties(rsmd, props, type);

		do {
			results.add(this.createBean(rs, type, props, columnToProperty));
		} while (rs.next());

		return results;
	}

	protected String getPropertyName(Method method) {
		String propertyName = null;
		if (method != null) {
			method.setAccessible(true);
			Column column = method.getAnnotation(Column.class);
			if (column != null) {
				if (StringUtils.hasText(column.value())) {
					propertyName = column.value();
				}
			}

			if (propertyName == null) {
				Id id = method.getAnnotation(Id.class);
				if (id != null) {
					if (StringUtils.hasText(id.value())) {
						propertyName = id.value();
					}
				}
			}
		}
		return propertyName;
	}

	protected String getProptertyName(Field field) {
		String propertyName = null;
		if (field != null) {
			field.setAccessible(true);
			Column column = field.getAnnotation(Column.class);
			if (column != null) {
				if (StringUtils.hasText(column.value())) {
					propertyName = column.value();
				}
			}

			if (propertyName == null) {
				Id id = field.getAnnotation(Id.class);
				if (id != null) {
					if (StringUtils.hasText(id.value())) {
						propertyName = id.value();
					}
				}
			}
		}
		return propertyName;
	}

	protected int[] mapColumnsToProperties(ResultSetMetaData rsmd, PropertyDescriptor[] props, Class<?> type)
			throws SQLException {
		int cols = rsmd.getColumnCount();
		int[] columnToProperty = new int[cols + 1];
		Arrays.fill(columnToProperty, PROPERTY_NOT_FOUND);

		for (int col = 1; col <= cols; col++) {
			String columnName = rsmd.getColumnLabel(col);
			if (null == columnName || 0 == columnName.length()) {
				columnName = rsmd.getColumnName(col);
			}

			for (int i = 0; i < props.length; i++) {
				PropertyDescriptor p = props[i];
				if ("class".equals(p.getName()))
					continue;

				String propertyName = null;
				try {
					Field field = type.getDeclaredField(p.getName());
					propertyName = getProptertyName(field);
				} catch (NoSuchFieldException e) {

				} catch (SecurityException e) {
					log.error("get annotation exception", e);
				}

				if (propertyName == null) {
					propertyName = getPropertyName(p.getReadMethod());
				}

				if (propertyName == null) {
					propertyName = getPropertyName(p.getWriteMethod());
				}

				if (propertyName == null) {
					propertyName = p.getName();

					final String generousColumnName = columnName.replace("_", "");

					if (columnName.equalsIgnoreCase(propertyName)
							|| generousColumnName.equalsIgnoreCase(propertyName)) {
						columnToProperty[col] = i;
						break;
					}
				} else {
					if (columnName.equalsIgnoreCase(propertyName)) {
						columnToProperty[col] = i;
						break;
					}
				}
			}
		}

		return columnToProperty;
	}

	/**
	 * Returns a PropertyDescriptor[] for the given Class.
	 *
	 * @param c
	 *            The Class to retrieve PropertyDescriptors for.
	 * @return A PropertyDescriptor[] describing the Class.
	 * @throws SQLException
	 *             if introspection failed.
	 */
	private PropertyDescriptor[] propertyDescriptors(Class<?> c) throws SQLException {
		// Introspector caches BeanInfo classes for better performance
		BeanInfo beanInfo = null;
		try {
			beanInfo = Introspector.getBeanInfo(c);

		} catch (IntrospectionException e) {
			throw new SQLException("Bean introspection failed: " + e.getMessage());
		}

		return beanInfo.getPropertyDescriptors();
	}

	private <T> T createBean(ResultSet rs, Class<T> type, PropertyDescriptor[] props, int[] columnToProperty)
			throws SQLException {

		T bean = this.newInstance(type);

		for (int i = 1; i < columnToProperty.length; i++) {

			if (columnToProperty[i] == PROPERTY_NOT_FOUND) {
				continue;
			}

			PropertyDescriptor prop = props[columnToProperty[i]];
			Class<?> propType = prop.getPropertyType();

			Object value = null;
			if (propType != null) {
				value = this.processColumn(rs, i, propType);

				if (value == null && propType.isPrimitive()) {
					value = primitiveDefaults.get(propType);
				}
			}

			this.callSetter(bean, prop, value);
		}

		return bean;
	}

	/**
	 * Calls the setter method on the target object for the given property. If
	 * no setter method exists for the property, this method does nothing.
	 * 
	 * @param target
	 *            The object to set the property on.
	 * @param prop
	 *            The property to set.
	 * @param value
	 *            The value to pass into the setter.
	 * @throws SQLException
	 *             if an error occurs setting the property.
	 */
	private void callSetter(Object target, PropertyDescriptor prop, Object value) throws SQLException {

		Method setter = prop.getWriteMethod();

		if (setter == null) {
			return;
		}

		Class<?>[] params = setter.getParameterTypes();
		try {
			// convert types for some popular ones
			if (value instanceof java.util.Date) {
				final String targetType = params[0].getName();
				if ("java.sql.Date".equals(targetType)) {
					value = new java.sql.Date(((java.util.Date) value).getTime());
				} else if ("java.sql.Time".equals(targetType)) {
					value = new java.sql.Time(((java.util.Date) value).getTime());
				} else if ("java.sql.Timestamp".equals(targetType)) {
					Timestamp tsValue = (Timestamp) value;
					int nanos = tsValue.getNanos();
					value = new java.sql.Timestamp(tsValue.getTime());
					((Timestamp) value).setNanos(nanos);
				}
			} else if (value instanceof String && params[0].isEnum()) {
				value = Enum.valueOf(params[0].asSubclass(Enum.class), (String) value);
			}

			// Don't call setter if the value object isn't the right type
			if (this.isCompatibleType(value, params[0])) {
				setter.invoke(target, new Object[] { value });
			} else {
				throw new SQLException("Cannot set " + prop.getName() + ": incompatible types, cannot convert "
						+ value.getClass().getName() + " to " + params[0].getName());
				// value cannot be null here because isCompatibleType allows
				// null
			}

		} catch (IllegalArgumentException e) {
			throw new SQLException("Cannot set " + prop.getName() + ": " + e.getMessage());

		} catch (IllegalAccessException e) {
			throw new SQLException("Cannot set " + prop.getName() + ": " + e.getMessage());

		} catch (InvocationTargetException e) {
			throw new SQLException("Cannot set " + prop.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * ResultSet.getObject() returns an Integer object for an INT column. The
	 * setter method for the property might take an Integer or a primitive int.
	 * This method returns true if the value can be successfully passed into the
	 * setter method. Remember, Method.invoke() handles the unwrapping of
	 * Integer into an int.
	 *
	 * @param value
	 *            The value to be passed into the setter method.
	 * @param type
	 *            The setter's parameter type (non-null)
	 * @return boolean True if the value is compatible (null => true)
	 */
	private boolean isCompatibleType(Object value, Class<?> type) {
		// Do object check first, then primitives
		if (value == null || type.isInstance(value)) {
			return true;

		} else if (type.equals(Integer.TYPE) && value instanceof Integer) {
			return true;

		} else if (type.equals(Long.TYPE) && value instanceof Long) {
			return true;

		} else if (type.equals(Double.TYPE) && value instanceof Double) {
			return true;

		} else if (type.equals(Float.TYPE) && value instanceof Float) {
			return true;

		} else if (type.equals(Short.TYPE) && value instanceof Short) {
			return true;

		} else if (type.equals(Byte.TYPE) && value instanceof Byte) {
			return true;

		} else if (type.equals(Character.TYPE) && value instanceof Character) {
			return true;

		} else if (type.equals(Boolean.TYPE) && value instanceof Boolean) {
			return true;

		}
		return false;

	}

}
