package com.firefly.db;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AnnotatedElement;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbutils.BeanProcessor;

import com.firefly.db.annotation.Column;
import com.firefly.db.annotation.Id;
import com.firefly.db.annotation.Table;
import com.firefly.utils.Assert;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;

public class DefaultBeanProcessor extends BeanProcessor {

	protected final ConcurrentReferenceHashMap<Class<?>, Map<String, Mapper>> mapperCache = new ConcurrentReferenceHashMap<>(
			128);
	protected final ConcurrentReferenceHashMap<Class<?>, SQLMapper> insertCache = new ConcurrentReferenceHashMap<>(128);
	protected final ConcurrentReferenceHashMap<Class<?>, SQLMapper> queryCache = new ConcurrentReferenceHashMap<>(128);
	protected final ConcurrentReferenceHashMap<Class<?>, SQLMapper> deleteCache = new ConcurrentReferenceHashMap<>(128);

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

	public SQLMapper generateDeleteSQL(Class<?> t) {
		return deleteCache.get(t, (key) -> {
			return _generateDeleteSQL(key);
		});
	}

	protected SQLMapper _generateDeleteSQL(Class<?> t) {
		SQLMapper sqlMapper = new SQLMapper();
		StringBuilder sql = new StringBuilder();
		String tableName = getTableName(t);
		String idColumnName = getIdColumnName(t);

		sql.append("delete from ").append(tableName).append(" where ").append(idColumnName).append(" = ?");
		sqlMapper.sql = sql.toString();
		return sqlMapper;
	}

	public SQLMapper generateQuerySQL(Class<?> t) {
		return queryCache.get(t, (key) -> {
			return _generateQuerySQL(key);
		});
	}

	protected SQLMapper _generateQuerySQL(Class<?> t) {
		SQLMapper sqlMapper = new SQLMapper();
		StringBuilder sql = new StringBuilder();

		sql.append("select");
		Map<String, Mapper> m = getMapper(t);
		m.forEach((property, mapper) -> {
			if (mapper.annotated) {
				sql.append(" ").append(mapper.columnName).append(",");
			}
		});
		sql.deleteCharAt(sql.length() - 1);

		String tableName = getTableName(t);
		String idColumnName = getIdColumnName(t);
		sql.append(" from ").append(tableName).append(" where ").append(idColumnName).append(" = ?");

		sqlMapper.sql = sql.toString();
		return sqlMapper;
	}

	public SQLMapper generateUpdateSQL(Class<?> t, Object object) {
		SQLMapper sqlMapper = new SQLMapper();
		StringBuilder sql = new StringBuilder();
		Map<String, Mapper> m = getMapper(t);
		List<Mapper> mapperList = new ArrayList<>();
		m.forEach((property, mapper) -> {
			if (!mapper.autoIncrement && mapper.annotated) {
				try {
					Object value = ReflectUtils.get(object, mapper.propertyName);
					if (value != null) {
						mapperList.add(mapper);
					}
				} catch (Throwable e) {
				}
			}
		});

		Map<String, Integer> propertyMap = new HashMap<>();
		String tableName = getTableName(t);
		Mapper idMapper = getIdMapper(t);
		Assert.notNull(idMapper, "id column must not be null");
		String idColumnName = idMapper.columnName;

		sql.append("update ").append(tableName).append(" set ");
		for (int i = 0; i < mapperList.size(); i++) {
			Mapper mapper = mapperList.get(i);
			if (i == 0) {
				sql.append(mapper.columnName).append(" = ?");
			} else {
				sql.append(", ").append(mapper.columnName).append(" = ?");
			}
			propertyMap.put(mapper.propertyName, i);
		}
		sql.append(" where ").append(idColumnName).append(" = ?");
		propertyMap.put(idMapper.propertyName, mapperList.size());

		sqlMapper.sql = sql.toString();
		sqlMapper.propertyMap = propertyMap;
		return sqlMapper;
	}

	public SQLMapper generateInsertSQL(Class<?> t) {
		return insertCache.get(t, (key) -> {
			return _generateInsertSQL(key);
		});
	}

	protected SQLMapper _generateInsertSQL(Class<?> t) {
		SQLMapper sqlMapper = new SQLMapper();
		StringBuilder sql = new StringBuilder();
		Map<String, Mapper> m = getMapper(t);
		List<Mapper> mapperList = new ArrayList<>();
		m.forEach((property, mapper) -> {
			if (!mapper.autoIncrement && mapper.annotated) {
				mapperList.add(mapper);
			}
		});

		Map<String, Integer> propertyMap = new HashMap<>();
		String tableName = getTableName(t);
		sql.append("insert into ").append(tableName).append(" (");
		for (int i = 0; i < mapperList.size(); i++) {
			Mapper mapper = mapperList.get(i);
			if (i == 0) {
				sql.append(mapper.columnName);
			} else {
				sql.append(", ").append(mapper.columnName);
			}
			propertyMap.put(mapper.propertyName, i);
		}
		sql.append(") values (");
		for (int i = 0; i < mapperList.size(); i++) {
			if (i == 0) {
				sql.append("?");
			} else {
				sql.append(", ?");
			}
		}
		sql.append(")");
		sqlMapper.sql = sql.toString();
		sqlMapper.propertyMap = propertyMap;
		return sqlMapper;
	}

	public static class SQLMapper {
		public String sql;
		public Map<String, Integer> propertyMap;

		@Override
		public String toString() {
			return "SQLMapper [sql=" + sql + ", propertyMap=" + propertyMap + "]";
		}

	}

	public String getTableName(Class<?> t) {
		Table table = t.getAnnotation(Table.class);
		if (table != null) {
			return table.value();
		} else {
			return t.getSimpleName();
		}
	}

	public String getIdColumnName(Class<?> t) {
		Mapper mapper = getIdMapper(t);
		if (mapper == null) {
			return null;
		} else {
			return mapper.columnName;
		}
	}

	public Mapper getIdMapper(Class<?> t) {
		Map<String, Mapper> map = getMapper(t);
		for (Map.Entry<String, Mapper> entry : map.entrySet()) {
			if (entry.getValue().idColumn) {
				return entry.getValue();
			}
		}
		return null;
	}

	public Map<String, Mapper> getMapper(Class<?> t) {
		return mapperCache.get(t, (clazz) -> {
			return _getMapper(clazz);
		});
	}

	private void idToMapper(Id id, Mapper mapper) {
		if (id != null) {
			if (StringUtils.hasText(id.value())) {
				mapper.columnName = id.value();
			}
			mapper.idColumn = true;
			mapper.autoIncrement = id.autoIncrement();
		}
	}

	private void columnToMapper(Column column, Mapper mapper) {
		if (column != null) {
			if (StringUtils.hasText(column.value())) {
				mapper.columnName = column.value();
			}
		}
	}

	private void annotationToMapper(AnnotatedElement e, Mapper mapper) {
		if (e != null) {
			idToMapper(e.getAnnotation(Id.class), mapper);

			if (mapper.columnName == null) {
				columnToMapper(e.getAnnotation(Column.class), mapper);
			}
		}
	}

	protected Map<String, Mapper> _getMapper(Class<?> t) {
		Map<String, Mapper> ret = new HashMap<>();
		Map<String, Method> getterMethodMap = ReflectUtils.getGetterMethods(t);
		Map<String, Method> setterMethodMap = ReflectUtils.getSetterMethods(t);
		Set<String> properties = new HashSet<>();

		getterMethodMap.forEach((property, method) -> {
			properties.add(property);
		});
		setterMethodMap.forEach((property, method) -> {
			properties.add(property);
		});

		properties.forEach((property) -> {
			Mapper mapper = new Mapper();
			mapper.propertyName = property;

			try {
				Field field = t.getDeclaredField(property);
				annotationToMapper(field, mapper);
			} catch (Exception e) {
			}

			if (mapper.columnName == null) {
				Method getterMethod = getterMethodMap.get(property);
				annotationToMapper(getterMethod, mapper);
			}

			if (mapper.columnName == null) {
				Method setterMethod = setterMethodMap.get(property);
				annotationToMapper(setterMethod, mapper);
			}

			if (mapper.columnName == null) {
				mapper.columnName = property;
			} else {
				mapper.annotated = true;
			}

			ret.put(property, mapper);
		});

		return ret;
	}

	public static class Mapper {
		public String propertyName;
		public String columnName;
		public boolean idColumn;
		public boolean autoIncrement;
		public boolean annotated;

		@Override
		public String toString() {
			return "Mapper [propertyName=" + propertyName + ", columnName=" + columnName + ", idColumn=" + idColumn
					+ ", autoIncrement=" + autoIncrement + ", annotated=" + annotated + "]";
		}

	}

	protected int[] mapColumnsToProperties(ResultSetMetaData rsmd, PropertyDescriptor[] props, Class<?> type)
			throws SQLException {
		int cols = rsmd.getColumnCount();
		int[] columnToProperty = new int[cols + 1];
		Arrays.fill(columnToProperty, PROPERTY_NOT_FOUND);

		Map<String, Mapper> map = getMapper(type);

		for (int col = 1; col <= cols; col++) {
			String columnName = rsmd.getColumnLabel(col);
			if (null == columnName || 0 == columnName.length()) {
				columnName = rsmd.getColumnName(col);
			}

			for (int i = 0; i < props.length; i++) {
				PropertyDescriptor p = props[i];
				if ("class".equals(p.getName()))
					continue;

				Mapper mapper = map.get(p.getName());
				if (mapper.annotated) {
					if (columnName.equalsIgnoreCase(mapper.columnName)) {
						columnToProperty[col] = i;
						break;
					}
				} else {
					final String generousColumnName = columnName.replace("_", "");

					if (columnName.equalsIgnoreCase(mapper.columnName)
							|| generousColumnName.equalsIgnoreCase(mapper.columnName)) {
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
