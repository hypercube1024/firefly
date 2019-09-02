package com.fireflysource.common.bytecode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractProxyFactory implements ProxyFactory {

    static final IdentityHashMap<Class<?>, String> primitiveWrapMap = new IdentityHashMap<>();
    public static ClassLoader classLoader;

    static {
        primitiveWrapMap.put(short.class, Short.class.getCanonicalName());
        primitiveWrapMap.put(byte.class, Byte.class.getCanonicalName());
        primitiveWrapMap.put(int.class, Integer.class.getCanonicalName());
        primitiveWrapMap.put(char.class, Character.class.getCanonicalName());
        primitiveWrapMap.put(float.class, Float.class.getCanonicalName());
        primitiveWrapMap.put(double.class, Double.class.getCanonicalName());
        primitiveWrapMap.put(long.class, Long.class.getCanonicalName());
        primitiveWrapMap.put(boolean.class, Boolean.class.getCanonicalName());

        classLoader = Thread.currentThread().getContextClassLoader();
    }

    protected final Map<Method, MethodProxy> methodCache = new ConcurrentHashMap<>();
    protected final Map<Field, FieldProxy> fieldCache = new ConcurrentHashMap<>();
    protected final Map<Class<?>, ArrayProxy> arrayCache = new ConcurrentHashMap<>();

    @Override
    public MethodProxy getMethodProxy(Method method) {
        return methodCache.computeIfAbsent(method, this::createMethodProxy);
    }

    abstract protected MethodProxy createMethodProxy(Method method);

    @Override
    public ArrayProxy getArrayProxy(Class<?> clazz) {
        return arrayCache.computeIfAbsent(clazz, this::createArrayProxy);
    }

    abstract protected ArrayProxy createArrayProxy(Class<?> clazz);

    @Override
    public FieldProxy getFieldProxy(Field field) {
        return fieldCache.computeIfAbsent(field, this::createFieldProxy);
    }

    abstract protected FieldProxy createFieldProxy(Field field);
}
