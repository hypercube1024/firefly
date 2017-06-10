package com.firefly.utils.json.support;

import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.annotation.Transient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Pengtao Qiu
 */
abstract public class PropertyUtils {

    public static boolean isTransientField(String propertyName, Class<?> clazz) {
        try {
            Field field = clazz.getDeclaredField(propertyName);
            return field != null && (Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class));
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static DateFormat getDateFormat(String propertyName, Class<?> clazz, Method method) {
        DateFormat d = null;
        try {
            Field field = clazz.getDeclaredField(propertyName);
            if(field != null) {
                d = field.getAnnotation(DateFormat.class);
            }
            if(d == null) {
                d = method.getAnnotation(DateFormat.class);
            }
        } catch (NoSuchFieldException ignore) {
        }
        return d;
    }

}
