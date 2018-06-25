package com.firefly.utils.json.support;

import com.firefly.utils.BeanUtils;
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
            Field field = BeanUtils.getField(propertyName, clazz);
            return field != null && (Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class));
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static boolean isTransientField(String propertyName, Class<?> clazz, Method setter, Method getter) {
        try {
            Field field = BeanUtils.getField(propertyName, clazz);
            return (field != null && (Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class)))
                    || (getter != null && getter.isAnnotationPresent(Transient.class))
                    || (setter != null && setter.isAnnotationPresent(Transient.class));
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static DateFormat getDateFormat(String propertyName, Class<?> clazz, Method setter, Method getter) {
        return getAnnotation(propertyName, clazz, setter, getter, DateFormat.class);
    }

    public static JsonProperty getJsonProperty(String propertyName, Class<?> clazz, Method setter, Method getter) {
        return getAnnotation(propertyName, clazz, setter, getter, JsonProperty.class);
    }

    public static <T extends Annotation> T getAnnotation(String propertyName, Class<?> clazz, Method setter, Method getter, Class<T> annotationClass) {
        T d;
        Field field = BeanUtils.getField(propertyName, clazz);
        if (field != null) {
            d = field.getAnnotation(annotationClass);
            if (d != null) {
                return d;
            }
        }
        if (setter != null) {
            d = setter.getAnnotation(annotationClass);
            if (d != null) {
                return d;
            }
        }
        if (getter != null) {
            d = getter.getAnnotation(annotationClass);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

}
