package com.firefly.utils;

import com.firefly.utils.classproxy.JavassistReflectionProxyFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ReflectUtils {

    private static final ConcurrentHashMap<Class<?>, Map<String, Method>> getterCache = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<Class<?>, Map<String, Method>> setterCache = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<Class<?>, Map<String, Field>> propertyCache = new ConcurrentHashMap<>(256);

    public static ProxyFactory defaultProxyFactory = JavassistReflectionProxyFactory.INSTANCE;

    public interface BeanMethodFilter {
        boolean accept(String propertyName, Method method);
    }

    public interface BeanFieldFilter {
        boolean accept(String propertyName, Field field);
    }

    public interface MethodProxy {

        Method method();

        /**
         * Executes this method
         *
         * @param obj  The instance of object that contains this method
         * @param args The parameters of this method
         * @return Return value of this method
         */
        Object invoke(Object obj, Object... args);
    }

    public interface FieldProxy {

        Field field();

        Object get(Object obj);

        void set(Object obj, Object value);
    }

    public interface ArrayProxy {
        int size(Object array);

        Object get(Object array, int index);

        void set(Object array, int index, Object value);
    }

    public interface ProxyFactory {
        ArrayProxy getArrayProxy(Class<?> clazz);

        FieldProxy getFieldProxy(Field field);

        MethodProxy getMethodProxy(Method method);
    }

    public static void setProperty(Object obj, String property, Object value) throws Throwable {
        getFields(obj.getClass()).get(property).set(obj, value);
    }

    public static Object getProperty(Object obj, String property) throws Throwable {
        return getFields(obj.getClass()).get(property).get(obj);
    }

    /**
     * Invokes a object's "setter" method by property name
     *
     * @param obj      The instance of a object
     * @param property The property name of this object
     * @param value    The parameter of "setter" method that you want to set
     * @throws Throwable A runtime exception
     */
    public static void set(Object obj, String property, Object value) throws Throwable {
        getSetterMethod(obj.getClass(), property).invoke(obj, value);
    }

    /**
     * Invokes a object's "getter" method by property name
     *
     * @param obj      The instance of a object
     * @param property The property name of this object
     * @return The value of this property
     * @throws Throwable A runtime exception
     */
    public static Object get(Object obj, String property) throws Throwable {
        return getGetterMethod(obj.getClass(), property).invoke(obj);
    }

    public static Object arrayGet(Object array, int index) {
        return getArrayProxy(array.getClass()).get(array, index);
    }

    public static void arraySet(Object array, int index, Object value) {
        getArrayProxy(array.getClass()).set(array, index, value);
    }

    public static int arraySize(Object array) {
        return getArrayProxy(array.getClass()).size(array);
    }

    public static ArrayProxy getArrayProxy(Class<?> clazz) {
        return defaultProxyFactory.getArrayProxy(clazz);
    }

    public static FieldProxy getFieldProxy(Field field) {
        return defaultProxyFactory.getFieldProxy(field);
    }

    public static MethodProxy getMethodProxy(Method method) {
        return defaultProxyFactory.getMethodProxy(method);
    }

    /**
     * Gets the all interface names of this class
     *
     * @param c The class of one object
     * @return Returns the all interface names
     */
    public static String[] getInterfaceNames(Class<?> c) {
        Class<?>[] interfaces = c.getInterfaces();
        List<String> names = new ArrayList<>();
        for (Class<?> i : interfaces) {
            names.add(i.getName());
        }
        return names.toArray(new String[0]);
    }

    public static String getPropertyName(Method method) {
        String methodName = method.getName();
        int index = (methodName.charAt(0) == 'i' ? 2 : 3);
        char c = methodName.charAt(index);
        if (Character.isLowerCase(c)) {
            return methodName.substring(index);
        } else {
            return Character.toLowerCase(methodName.charAt(index)) + methodName.substring(index + 1);
        }
    }

    public static Method getSetterMethod(Class<?> clazz, String propertyName) {
        return getSetterMethods(clazz).get(propertyName);
    }

    public static Map<String, Method> getSetterMethods(Class<?> clazz) {
        return setterCache.computeIfAbsent(clazz, key -> getSetterMethods(key, null));
    }

    public static Map<String, Method> getSetterMethods(Class<?> clazz, BeanMethodFilter filter) {
        Map<String, Method> setMethodMap = new HashMap<>();
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            method.setAccessible(true);
            if (method.getName().length() < 4
                    || !method.getName().startsWith("set")
                    || Modifier.isStatic(method.getModifiers())
                    || !method.getReturnType().equals(Void.TYPE)
                    || method.getParameterTypes().length != 1)
                continue;

            String propertyName = getPropertyName(method);
            if (filter == null || filter.accept(propertyName, method))
                setMethodMap.put(propertyName, method);
        }
        return setMethodMap;
    }

    public static Method getGetterMethod(Class<?> clazz, String propertyName) {
        return getGetterMethods(clazz).get(propertyName);
    }

    public static Map<String, Method> getGetterMethods(Class<?> clazz) {
        return getterCache.computeIfAbsent(clazz, key -> getGetterMethods(key, null));
    }

    public static Map<String, Method> getGetterMethods(Class<?> clazz, BeanMethodFilter filter) {
        Map<String, Method> getMethodMap = new HashMap<>();
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            method.setAccessible(true);
            String methodName = method.getName();

            if (Modifier.isStatic(method.getModifiers()))
                continue;
            if (Modifier.isAbstract(method.getModifiers()))
                continue;
            if (method.getName().equals("getClass"))
                continue;
            if (!(method.getName().startsWith("is") || method.getName().startsWith("get")))
                continue;
            if (method.getParameterTypes().length != 0)
                continue;
            if (method.getReturnType() == void.class)
                continue;
            int index = (methodName.charAt(0) == 'i' ? 2 : 3);
            if (methodName.length() < index + 1)
                continue;

            String propertyName = getPropertyName(method);
            if (filter == null || filter.accept(propertyName, method))
                getMethodMap.put(propertyName, method);
        }

        return getMethodMap;
    }

    public static Map<String, Field> getFields(Class<?> clazz) {
        return propertyCache.computeIfAbsent(clazz, key -> getFields(key, null));
    }

    public static Map<String, Field> getFields(Class<?> clazz, BeanFieldFilter filter) {
        Map<String, Field> fieldMap = new HashMap<>();
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers()))
                continue;

            String propertyName = field.getName();
            if (filter == null || filter.accept(propertyName, field))
                fieldMap.put(propertyName, field);
        }
        return fieldMap;
    }

    public static void copy(Object src, Object dest) {
        Map<String, Method> getterMethodMap = ReflectUtils.getGetterMethods(src.getClass());
        Map<String, Method> setterMethodMap = ReflectUtils.getSetterMethods(dest.getClass());

        for (Map.Entry<String, Method> entry : setterMethodMap.entrySet()) {
            Method getter = getterMethodMap.get(entry.getKey());
            if (getter == null)
                continue;

            try {
                Object obj = getter.invoke(src);
                if (obj != null) {
                    entry.getValue().invoke(dest, obj);
                }
            } catch (Throwable t) {
                System.err.println("copy object exception, " + t.getMessage());
            }
        }
    }
}
