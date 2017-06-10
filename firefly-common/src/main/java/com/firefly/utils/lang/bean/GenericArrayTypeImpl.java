package com.firefly.utils.lang.bean;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

/**
 * @author Pengtao Qiu
 */
public class GenericArrayTypeImpl implements GenericArrayType {

    private Type genericComponentType;

    @Override
    public Type getGenericComponentType() {
        return genericComponentType;
    }

    public void setGenericComponentType(Type genericComponentType) {
        this.genericComponentType = genericComponentType;
    }

    @Override
    public String toString() {
        return genericComponentType.getTypeName() + "[]";
    }
}
