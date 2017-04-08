package com.firefly.utils.classproxy;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ProxyFactory;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;
import com.firefly.utils.collection.IdentityHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractProxyFactory implements ProxyFactory {

    protected final ConcurrentHashMap<Method, ReflectUtils.MethodProxy> methodCache = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Field, ReflectUtils.FieldProxy> fieldCache = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Class<?>, ReflectUtils.ArrayProxy> arrayCache = new ConcurrentHashMap<>();

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

    @Override
    public ReflectUtils.MethodProxy getMethodProxy(Method method) {
        return methodCache.computeIfAbsent(method, this::_getMethodProxy);
    }

    abstract protected ReflectUtils.MethodProxy _getMethodProxy(Method method);

    @Override
    public ReflectUtils.ArrayProxy getArrayProxy(Class<?> clazz) {
        return arrayCache.computeIfAbsent(clazz, this::_getArrayProxy);
    }

    abstract protected ReflectUtils.ArrayProxy _getArrayProxy(Class<?> clazz);

    @Override
    public ReflectUtils.FieldProxy getFieldProxy(Field field) {
        return fieldCache.computeIfAbsent(field, this::_getFieldProxy);
    }

    abstract protected ReflectUtils.FieldProxy _getFieldProxy(Field field);
}
