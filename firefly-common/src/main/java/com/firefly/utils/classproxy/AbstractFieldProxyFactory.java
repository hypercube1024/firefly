package com.firefly.utils.classproxy;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

abstract public class AbstractFieldProxyFactory extends AbstractProxyFactory {

    protected final Map<Field, ReflectUtils.FieldProxy> fieldCache = new ConcurrentReferenceHashMap<>(256);

    @Override
    public ArrayProxy getArrayProxy(Class<?> clazz) throws Throwable {
        return null;
    }

    @Override
    public MethodProxy getMethodProxy(Method method) throws Throwable {
        return null;
    }

    abstract protected ReflectUtils.FieldProxy _getFieldProxy(Field field) throws Throwable;

    public ReflectUtils.FieldProxy getFieldProxy(Field field) throws Throwable {
        ReflectUtils.FieldProxy ret = fieldCache.get(field);
        if (ret != null)
            return ret;

        synchronized (fieldCache) {
            ret = fieldCache.get(field);
            if (ret != null)
                return ret;

            ret = _getFieldProxy(field);
            fieldCache.put(field, ret);
            return ret;
        }
    }

}
