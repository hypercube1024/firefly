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

    Class<?> extractClass();

    boolean isArray();

    <T> void setValue(Object instance, T value);

    <T> T getValue(Object instance);

    Field getField();

    Method getGetterMethod();

    Method getSetterMethod();
}
