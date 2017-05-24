package com.firefly.core.support.annotation;

import com.firefly.annotation.DestroyedMethod;
import com.firefly.annotation.InitialMethod;
import com.firefly.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
abstract public class AnnotationBeanUtils {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public static Method getDestroyedMethod(Class<?> c) {
        Method[] methods = c.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(DestroyedMethod.class)) {
                return m;
            }
        }
        return null;
    }

    public static Method getInitMethod(Class<?> c) {
        Method[] methods = c.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(InitialMethod.class)) {
                return m;
            }
        }
        return null;
    }

    public static Constructor<?> getInjectConstructor(Class<?> c) {
        for (Constructor<?> constructor : c.getConstructors()) {
            if (constructor.getAnnotation(Inject.class) != null) {
                return constructor;
            }
        }
        try {
            return c.getConstructor();
        } catch (Throwable t) {
            log.error("gets non-parameter constructor error", t);
            return null;
        }
    }

    public static List<Field> getInjectFields(Class<?> c) {
        Field[] fields = c.getDeclaredFields();
        List<Field> list = new ArrayList<>();
        for (Field field : fields) {
            if (field.getAnnotation(Inject.class) != null) {
                list.add(field);
            }
        }
        return list;
    }

    public static List<Method> getInjectMethods(Class<?> c) {
        Method[] methods = c.getDeclaredMethods();
        List<Method> list = new ArrayList<>();
        for (Method m : methods) {
            if (m.isAnnotationPresent(Inject.class)) {
                list.add(m);
            }
        }
        return list;
    }
}
