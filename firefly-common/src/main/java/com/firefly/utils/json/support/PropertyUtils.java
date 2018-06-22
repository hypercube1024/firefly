package com.firefly.utils.json.support;

import com.firefly.utils.json.annotation.DateFormat;
import com.firefly.utils.json.annotation.JsonProperty;
import com.firefly.utils.json.annotation.Transient;

import java.lang.annotation.Annotation;
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

    public static boolean isTransientField(String propertyName, Class<?> clazz, Method setter, Method getter) {
        try {
            Field field = clazz.getDeclaredField(propertyName);
            return (field != null && (Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class)))
                    || (getter != null && getter.isAnnotationPresent(Transient.class))
                    || (setter != null && setter.isAnnotationPresent(Transient.class));
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static DateFormat getDateFormat(String propertyName, Class<?> clazz, Method method) {
        DateFormat d = null;
        try {
            Field field = clazz.getDeclaredField(propertyName);
            if (field != null) {
                d = field.getAnnotation(DateFormat.class);
            }
            if (d == null) {
                d = method.getAnnotation(DateFormat.class);
            }
        } catch (NoSuchFieldException ignore) {
        }
        return d;
    }

    public static DateFormat getDateFormat(String propertyName, Class<?> clazz, Method setter, Method getter) {
        return getAnnotation(propertyName, clazz, setter, getter, DateFormat.class);
    }

    public static JsonProperty getJsonProperty(String propertyName, Class<?> clazz, Method setter, Method getter) {
        return getAnnotation(propertyName, clazz, setter, getter, JsonProperty.class);
    }

    public static <T extends Annotation> T getAnnotation(String propertyName, Class<?> clazz, Method setter, Method getter, Class<T> annotationClass) {
        T d = null;
        try {
            Field field = clazz.getDeclaredField(propertyName);
            if (field != null) {
                d = field.getAnnotation(annotationClass);
            }
            if (d == null) {
                if (setter != null) {
                    d = setter.getAnnotation(annotationClass);
                }
            }
            if (d == null) {
                if (getter != null) {
                    d = getter.getAnnotation(annotationClass);
                }
            }
        } catch (NoSuchFieldException ignore) {
        }
        return d;
    }

}
