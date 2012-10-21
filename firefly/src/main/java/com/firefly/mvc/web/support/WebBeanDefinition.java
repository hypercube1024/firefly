package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.List;

import com.firefly.core.support.annotation.AnnotationBeanDefinition;

public interface WebBeanDefinition extends AnnotationBeanDefinition {
	List<Method> getReqMethods();

	void setReqMethods(List<Method> reqMethods);
}
