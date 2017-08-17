package com.firefly.db.jdbc.helper;

import com.firefly.db.annotation.Column;
import com.firefly.db.annotation.Id;
import com.firefly.db.annotation.Table;
import com.firefly.utils.Assert;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.PropertyHandler;

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
import java.util.*;

public class DefaultBeanProcessor extends BeanProcessor {

    private final ConcurrentReferenceHashMap<Class<?>, Map<String, Mapper>> mapperCache = new ConcurrentReferenceHashMap<>(128);
    private final ConcurrentReferenceHashMap<Class<?>, SQLMapper> insertCache = new ConcurrentReferenceHashMap<>(128);
    private final ConcurrentReferenceHashMap<Class<?>, SQLMapper> queryCache = new ConcurrentReferenceHashMap<>(128);
    private final ConcurrentReferenceHashMap<Class<?>, SQLMapper> deleteCache = new ConcurrentReferenceHashMap<>(128);

    /**
     * Set a bean's primitive properties to these defaults when SQL NULL is
     * returned. These are the same as the defaults that ResultSet get* methods
     * return in the event of a NULL column.
     */
    private static final Map<Class<?>, Object> primitiveDefaults = new HashMap<>();

    /**
     * ServiceLoader to find <code>PropertyHandler</code> implementations on the classpath.  The iterator for this is
     * lazy and each time <code>iterator()</code> is called.
     */
    // FIXME: I think this instantiates new handlers on each iterator() call. This might be worth caching upfront.
    private static final ServiceLoader<PropertyHandler> propertyHandlers = ServiceLoader.load(PropertyHandler.class);

    static {
        primitiveDefaults.put(Integer.TYPE, 0);
        primitiveDefaults.put(Short.TYPE, (short) 0);
        primitiveDefaults.put(Byte.TYPE, (byte) 0);
        primitiveDefaults.put(Float.TYPE, 0f);
        primitiveDefaults.put(Double.TYPE, 0d);
        primitiveDefaults.put(Long.TYPE, 0L);
        primitiveDefaults.put(Boolean.TYPE, Boolean.FALSE);
        primitiveDefaults.put(Character.TYPE, (char) 0);
    }

    @Override
    public <T> T toBean(ResultSet rs, Class<? extends T> type) throws SQLException {
        T bean = this.newInstance(type);
        return this.populateBean(rs, bean);
    }

    @Override
    public <T> List<T> toBeanList(ResultSet rs, Class<? extends T> type) throws SQLException {
        List<T> results = new ArrayList<>();

        if (!rs.next()) {
            return results;
        }

        PropertyDescriptor[] props = this.propertyDescriptors(type);
        int[] columnToProperty = this.mapColumnsToProperties(rs.getMetaData(), props, type);

        do {
            results.add(this.createBean(rs, type, props, columnToProperty));
        } while (rs.next());

        return results;
    }

    /**
     * Creates a new object and initializes its fields from the ResultSet.
     *
     * @param <T>              The type of bean to create
     * @param rs               The result set.
     * @param type             The bean type (the return type of the object).
     * @param props            The property descriptors.
     * @param columnToProperty The column indices in the result set.
     * @return An initialized object.
     * @throws SQLException if a database error occurs.
     */
    private <T> T createBean(ResultSet rs, Class<T> type,
                             PropertyDescriptor[] props, int[] columnToProperty)
            throws SQLException {

        T bean = this.newInstance(type);
        return populateBean(rs, bean, props, columnToProperty);
    }

    /**
     * Initializes the fields of the provided bean from the ResultSet.
     *
     * @param <T>  The type of bean
     * @param rs   The result set.
     * @param bean The bean to be populated.
     * @return An initialized object.
     * @throws SQLException if a database error occurs.
     */
    @Override
    public <T> T populateBean(ResultSet rs, T bean) throws SQLException {
        Class<?> type = bean.getClass();
        PropertyDescriptor[] props = this.propertyDescriptors(type);
        int[] columnToProperty = this.mapColumnsToProperties(rs.getMetaData(), props, type);
        return populateBean(rs, bean, props, columnToProperty);
    }

    /**
     * This method populates a bean from the ResultSet based upon the underlying meta-data.
     *
     * @param <T>              The type of bean
     * @param rs               The result set.
     * @param bean             The bean to be populated.
     * @param props            The property descriptors.
     * @param columnToProperty The column indices in the result set.
     * @return An initialized object.
     * @throws SQLException if a database error occurs.
     */
    private <T> T populateBean(ResultSet rs, T bean,
                               PropertyDescriptor[] props, int[] columnToProperty)
            throws SQLException {

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
     * Calls the setter method on the target object for the given property.
     * If no setter method exists for the property, this method does nothing.
     *
     * @param target The object to set the property on.
     * @param prop   The property to set.
     * @param value  The value to pass into the setter.
     * @throws SQLException if an error occurs setting the property.
     */
    private void callSetter(Object target, PropertyDescriptor prop, Object value)
            throws SQLException {

        Method setter = getWriteMethod(target, prop, value);

        if (setter == null || setter.getParameterTypes().length != 1) {
            return;
        }

        try {
            Class<?> firstParam = setter.getParameterTypes()[0];
            for (PropertyHandler handler : propertyHandlers) {
                if (handler.match(firstParam, value)) {
                    value = handler.apply(firstParam, value);
                    break;
                }
            }

            // Don't call setter if the value object isn't the right type
            if (this.isCompatibleType(value, firstParam)) {
                setter.invoke(target, new Object[]{value});
            } else {
                throw new SQLException(
                        "Cannot set " + prop.getName() + ": incompatible types, cannot convert "
                                + value.getClass().getName() + " to " + firstParam.getName());
                // value cannot be null here because isCompatibleType allows null
            }

        } catch (IllegalArgumentException e) {
            throw new SQLException(
                    "Cannot set " + prop.getName() + ": " + e.getMessage());

        } catch (IllegalAccessException e) {
            throw new SQLException(
                    "Cannot set " + prop.getName() + ": " + e.getMessage());

        } catch (InvocationTargetException e) {
            throw new SQLException(
                    "Cannot set " + prop.getName() + ": " + e.getMessage());
        }
    }

    /**
     * ResultSet.getObject() returns an Integer object for an INT column.  The
     * setter method for the property might take an Integer or a primitive int.
     * This method returns true if the value can be successfully passed into
     * the setter method.  Remember, Method.invoke() handles the unwrapping
     * of Integer into an int.
     *
     * @param value The value to be passed into the setter method.
     * @param type  The setter's parameter type (non-null)
     * @return boolean True if the value is compatible (null => true)
     */
    private boolean isCompatibleType(Object value, Class<?> type) {
        // Do object check first, then primitives
        if (value == null || type.isInstance(value) || matchesPrimitive(type, value.getClass())) {
            return true;

        }
        return false;

    }

    /**
     * Check whether a value is of the same primitive type as <code>targetType</code>.
     *
     * @param targetType The primitive type to target.
     * @param valueType  The value to match to the primitive type.
     * @return Whether <code>valueType</code> can be coerced (e.g. autoboxed) into <code>targetType</code>.
     */
    private boolean matchesPrimitive(Class<?> targetType, Class<?> valueType) {
        if (!targetType.isPrimitive()) {
            return false;
        }

        try {
            // see if there is a "TYPE" field.  This is present for primitive wrappers.
            Field typeField = valueType.getField("TYPE");
            Object primitiveValueType = typeField.get(valueType);

            if (targetType == primitiveValueType) {
                return true;
            }
        } catch (NoSuchFieldException e) {
            // lacking the TYPE field is a good sign that we're not working with a primitive wrapper.
            // we can't match for compatibility
        } catch (IllegalAccessException e) {
            // an inaccessible TYPE field is a good sign that we're not working with a primitive wrapper.
            // nothing to do.  we can't match for compatibility
        }
        return false;
    }

    /**
     * Returns a PropertyDescriptor[] for the given Class.
     *
     * @param c The Class to retrieve PropertyDescriptors for.
     * @return A PropertyDescriptor[] describing the Class.
     * @throws SQLException if introspection failed.
     */
    private PropertyDescriptor[] propertyDescriptors(Class<?> c)
            throws SQLException {
        // Introspector caches BeanInfo classes for better performance
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(c);

        } catch (IntrospectionException e) {
            throw new SQLException(
                    "Bean introspection failed: " + e.getMessage());
        }

        return beanInfo.getPropertyDescriptors();
    }

    public SQLMapper generateDeleteSQL(Class<?> t) {
        return deleteCache.get(t, this::_generateDeleteSQL);
    }

    private SQLMapper _generateDeleteSQL(Class<?> t) {
        SQLMapper sqlMapper = new SQLMapper();
        StringBuilder sql = new StringBuilder();
        String tableName = getTableName(t);
        String catalog = getCatalog(t);
        String idColumnName = getIdColumnName(t);

        sql.append("delete from ");
        if (StringUtils.hasText(catalog)) {
            sql.append(" `").append(catalog).append("`.").append("`").append(tableName).append("` ");
        } else {
            sql.append(" `").append(tableName).append("` ");
        }
        sql.append(" where `").append(idColumnName).append("` = ?");
        sqlMapper.sql = sql.toString();
        return sqlMapper;
    }

    public SQLMapper generateQuerySQL(Class<?> t) {
        return queryCache.get(t, this::_generateQuerySQL);
    }

    private SQLMapper _generateQuerySQL(Class<?> t) {
        SQLMapper sqlMapper = new SQLMapper();
        StringBuilder sql = new StringBuilder();

        sql.append("select");
        Map<String, Mapper> m = getMapper(t);
        m.forEach((property, mapper) -> {
            if (mapper.annotated) {
                sql.append(" `").append(mapper.columnName).append("`,");
            }
        });
        sql.deleteCharAt(sql.length() - 1);

        String tableName = getTableName(t);
        String catalog = getCatalog(t);
        String idColumnName = getIdColumnName(t);
        sql.append(" from ");
        if (StringUtils.hasText(catalog)) {
            sql.append(" `").append(catalog).append("`.").append("`").append(tableName).append("` ");
        } else {
            sql.append(" `").append(tableName).append("` ");
        }
        sql.append(" where `").append(idColumnName).append("` = ?");

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
                } catch (Throwable ignored) {
                }
            }
        });

        Map<String, Integer> propertyMap = new HashMap<>();
        String tableName = getTableName(t);
        String catalog = getCatalog(t);
        Mapper idMapper = getIdMapper(t);
        Assert.notNull(idMapper, "id column must not be null");
        String idColumnName = idMapper.columnName;

        sql.append("update ");
        if (StringUtils.hasText(catalog)) {
            sql.append(" `").append(catalog).append("`.").append("`").append(tableName).append("` ");
        } else {
            sql.append(" `").append(tableName).append("` ");
        }
        sql.append(" set ");
        for (int i = 0; i < mapperList.size(); i++) {
            Mapper mapper = mapperList.get(i);
            if (i == 0) {
                sql.append('`').append(mapper.columnName).append("` = ?");
            } else {
                sql.append(", `").append(mapper.columnName).append("` = ?");
            }
            propertyMap.put(mapper.propertyName, i);
        }
        sql.append(" where `").append(idColumnName).append("` = ?");
        propertyMap.put(idMapper.propertyName, mapperList.size());

        sqlMapper.sql = sql.toString();
        sqlMapper.propertyMap = propertyMap;
        return sqlMapper;
    }

    public SQLMapper generateInsertSQL(Class<?> t) {
        return insertCache.get(t, this::_generateInsertSQL);
    }

    private SQLMapper _generateInsertSQL(Class<?> t) {
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
        String catalog = getCatalog(t);

        sql.append("insert into ");
        if (StringUtils.hasText(catalog)) {
            sql.append(" `").append(catalog).append("`.").append("`").append(tableName).append("` ");
        } else {
            sql.append(" `").append(tableName).append("` ");
        }
        sql.append(" (");
        for (int i = 0; i < mapperList.size(); i++) {
            Mapper mapper = mapperList.get(i);
            if (i == 0) {
                sql.append('`').append(mapper.columnName).append('`');
            } else {
                sql.append(", `").append(mapper.columnName).append('`');
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

    public String getCatalog(Class<?> t) {
        Table table = t.getAnnotation(Table.class);
        if (table != null) {
            return table.catalog();
        } else {
            return null;
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
        return mapperCache.get(t, this::_getMapper);
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

    private Map<String, Mapper> _getMapper(Class<?> t) {
        Map<String, Mapper> ret = new HashMap<>();
        Map<String, Method> getterMethodMap = ReflectUtils.getGetterMethods(t);
        Map<String, Method> setterMethodMap = ReflectUtils.getSetterMethods(t);
        Set<String> properties = new HashSet<>();

        getterMethodMap.forEach((property, method) -> properties.add(property));
        setterMethodMap.forEach((property, method) -> properties.add(property));

        properties.forEach((property) -> {
            Mapper mapper = new Mapper();
            mapper.propertyName = property;

            try {
                Field field = t.getDeclaredField(property);
                annotationToMapper(field, mapper);
            } catch (Exception ignored) {
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

    protected int[] mapColumnsToProperties(ResultSetMetaData rsmd, PropertyDescriptor[] props, Class<?> type) throws SQLException {
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

}
