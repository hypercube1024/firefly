package com.firefly.utils.json.support;

import com.firefly.utils.collection.IdentityHashMap;
import com.firefly.utils.json.annotation.CircularReferenceCheck;

import java.util.Collection;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public enum ClassType {
    ENUM, COLLECTION, MAP, ARRAY, OBJECT, NO_CHECK_OBJECT;

    private static final IdentityHashMap<Class<?>, ClassType> CLASS_TYPE_MAP = new IdentityHashMap<>();

    public static ClassType getClassType(Class<?> clazz) {
        ClassType classType = CLASS_TYPE_MAP.get(clazz);
        if (classType == null) {
            if (clazz.isEnum()) {
                classType = ClassType.ENUM;
            } else if (Map.class.isAssignableFrom(clazz)) {
                classType = ClassType.MAP;
            } else if (Collection.class.isAssignableFrom(clazz)) {
                classType = ClassType.COLLECTION;
            } else if (clazz.isArray()) {
                classType = ClassType.ARRAY;
            } else {
                if (clazz.isAnnotationPresent(CircularReferenceCheck.class)) {
                    classType = ClassType.OBJECT;
                } else {
                    classType = ClassType.NO_CHECK_OBJECT;
                }
            }
            CLASS_TYPE_MAP.put(clazz, classType);
        }
        return classType;
    }
}
