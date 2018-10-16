package com.firefly.core.support.annotation;

import com.firefly.core.support.BeanDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public interface AnnotationBeanDefinition extends BeanDefinition {

    List<Field> getInjectFields();

    void setInjectFields(List<Field> fields);

    List<Method> getInjectMethods();

    void setInjectMethods(List<Method> methods);

}
