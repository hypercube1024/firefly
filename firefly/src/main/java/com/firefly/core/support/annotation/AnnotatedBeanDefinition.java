package com.firefly.core.support.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class AnnotatedBeanDefinition implements AnnotationBeanDefinition {

    private String id;
    private String className;
    private String[] interfaceNames;
    private List<Field> injectFields;
    private List<Method> injectMethods;
    private Constructor<?> constructor;
    private Method initMethod;
    private Method destroyedMethod;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    @Override
    public void setInterfaceNames(String[] interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    @Override
    public List<Field> getInjectFields() {
        return injectFields;
    }

    @Override
    public void setInjectFields(List<Field> injectFields) {
        this.injectFields = injectFields;
    }

    @Override
    public List<Method> getInjectMethods() {
        return injectMethods;
    }

    @Override
    public void setInjectMethods(List<Method> injectMethods) {
        this.injectMethods = injectMethods;
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Override
    public void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    @Override
    public Method getInitMethod() {
        return initMethod;
    }

    @Override
    public void setInitMethod(Method initMethod) {
        this.initMethod = initMethod;
    }

    @Override
    public Method getDestroyedMethod() {
        return destroyedMethod;
    }

    @Override
    public void setDestroyedMethod(Method destroyedMethod) {
        this.destroyedMethod = destroyedMethod;
    }


}
