package com.fireflysource.common.bytecode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Pengtao Qiu
 */
public interface ProxyFactory {
    ArrayProxy getArrayProxy(Class<?> clazz);

    FieldProxy getFieldProxy(Field field);

    MethodProxy getMethodProxy(Method method);
}
