package com.firefly.utils.classproxy;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.FieldProxy;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

abstract public class AbstractArrayProxyFactory extends AbstractProxyFactory {

    protected final Map<Class<?>, ReflectUtils.ArrayProxy> arrayCache = new ConcurrentReferenceHashMap<>(256);

    @Override
    public FieldProxy getFieldProxy(Field field) throws Throwable {
        return null;
    }

    @Override
    public MethodProxy getMethodProxy(Method method) throws Throwable {
        return null;
    }

    @Override
    public ReflectUtils.ArrayProxy getArrayProxy(Class<?> clazz) throws Throwable {
        if (!clazz.isArray())
            throw new IllegalArgumentException("type error, it's not array");

        ReflectUtils.ArrayProxy ret = arrayCache.get(clazz);
        if (ret != null)
            return ret;

        synchronized (arrayCache) {
            ret = arrayCache.get(clazz);
            if (ret != null)
                return ret;

            ret = _getArrayProxy(clazz);
            arrayCache.put(clazz, ret);
            return ret;
        }
    }

    abstract protected ReflectUtils.ArrayProxy _getArrayProxy(Class<?> clazz) throws Throwable;
}
