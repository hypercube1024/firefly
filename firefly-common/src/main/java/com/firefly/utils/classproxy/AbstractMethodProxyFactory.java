package com.firefly.utils.classproxy;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.ReflectUtils.FieldProxy;
import com.firefly.utils.collection.ConcurrentReferenceHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

abstract public class AbstractMethodProxyFactory extends AbstractProxyFactory {

    protected final Map<Method, ReflectUtils.MethodProxy> methodCache = new ConcurrentReferenceHashMap<>(256);

    @Override
    public ArrayProxy getArrayProxy(Class<?> clazz) throws Throwable {
        return null;
    }

    @Override
    public FieldProxy getFieldProxy(Field field) throws Throwable {
        return null;
    }

    @Override
    public ReflectUtils.MethodProxy getMethodProxy(Method method) throws Throwable {
        ReflectUtils.MethodProxy ret = methodCache.get(method);
        if (ret != null)
            return ret;

        synchronized (methodCache) {
            ret = methodCache.get(method);
            if (ret != null)
                return ret;

            ret = _getMethodProxy(method);
            methodCache.put(method, ret);
            return ret;
        }
    }

    abstract protected ReflectUtils.MethodProxy _getMethodProxy(Method method) throws Throwable;

}
