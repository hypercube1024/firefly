package com.firefly.utils.lang.bean;

import com.firefly.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Pengtao Qiu
 */
public class PropertyAccessImpl implements PropertyAccess {

    private String name;
    private Type type;
    private Field field;
    private Method getterMethod;
    private Method setterMethod;
    private ReflectUtils.MethodProxy setterMethodProxy;
    private ReflectUtils.MethodProxy getterMethodProxy;
    private ReflectUtils.FieldProxy fieldProxy;


    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setValue(Object instance, Object value) {
        if (setterMethodProxy != null) {
            setterMethodProxy.invoke(instance, value);
        } else if (fieldProxy != null) {
            fieldProxy.set(instance, value);
        }
    }

    @Override
    public Object getValue(Object instance) {
        if (getterMethodProxy != null) {
            return getterMethodProxy.invoke(instance);
        } else if (fieldProxy != null) {
            return fieldProxy.get(instance);
        } else {
            return null;
        }
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    @Override
    public Method getGetterMethod() {
        return getterMethod;
    }

    public void setGetterMethod(Method getterMethod) {
        this.getterMethod = getterMethod;
    }

    @Override
    public Method getSetterMethod() {
        return setterMethod;
    }

    public void setSetterMethod(Method setterMethod) {
        this.setterMethod = setterMethod;
    }

    public ReflectUtils.MethodProxy getSetterMethodProxy() {
        return setterMethodProxy;
    }

    public void setSetterMethodProxy(ReflectUtils.MethodProxy setterMethodProxy) {
        this.setterMethodProxy = setterMethodProxy;
    }

    public ReflectUtils.MethodProxy getGetterMethodProxy() {
        return getterMethodProxy;
    }

    public void setGetterMethodProxy(ReflectUtils.MethodProxy getterMethodProxy) {
        this.getterMethodProxy = getterMethodProxy;
    }

    public ReflectUtils.FieldProxy getFieldProxy() {
        return fieldProxy;
    }

    public void setFieldProxy(ReflectUtils.FieldProxy fieldProxy) {
        this.fieldProxy = fieldProxy;
    }
}
