package com.firefly.utils.lang.bean;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Pengtao Qiu
 */
public class ParameterizedTypeImpl implements ParameterizedType {

    private Type rawType;
    private Type[] actualTypeArguments;
    private Type ownerType;

    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    public void setRawType(Type rawType) {
        this.rawType = rawType;
    }

    public void setActualTypeArguments(Type[] actualTypeArguments) {
        this.actualTypeArguments = actualTypeArguments;
    }

    public void setOwnerType(Type ownerType) {
        this.ownerType = ownerType;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(rawType.getTypeName()).append('<');
        for (Type t : actualTypeArguments) {
            str.append(t.getTypeName()).append(", ");
        }
        str.delete(str.length() - 2, str.length());
        str.append('>');
        return str.toString();
    }
}
