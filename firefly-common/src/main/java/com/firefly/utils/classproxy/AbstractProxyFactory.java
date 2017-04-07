package com.firefly.utils.classproxy;

import com.firefly.utils.ReflectUtils.ProxyFactory;
import com.firefly.utils.collection.IdentityHashMap;

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
}
