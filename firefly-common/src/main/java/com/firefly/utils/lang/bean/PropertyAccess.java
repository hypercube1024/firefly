package com.firefly.utils.lang.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Pengtao Qiu
 */
public interface PropertyAccess {

    String getName();

    Type getType();

    void setValue(Object instance, Object value);

    Object getValue(Object instance);

    Field getField();

    Method getGetterMethod();

    Method getSetterMethod();
}
