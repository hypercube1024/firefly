package com.firefly.mvc.web.support;

import java.lang.reflect.Method;

import com.firefly.core.support.annotation.AnnotationBeanDefinition;

public interface InterceptorBeanDefinition extends AnnotationBeanDefinition {
	Method getDisposeMethod();
	
	void setDisposeMethod(Method method);
	
	String getUriPattern();
	
	void setUriPattern(String uriPattern);
	
	int getOrder();
	
	void setOrder(int order);
}
