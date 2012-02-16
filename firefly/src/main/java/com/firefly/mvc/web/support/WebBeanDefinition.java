package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.List;

import com.firefly.core.support.annotation.AnnotationBeanDefinition;

public interface WebBeanDefinition extends AnnotationBeanDefinition {
	List<Method> getReqMethods();

	void setReqMethods(List<Method> reqMethods);

	List<Method> getInterceptorMethods();

	void setInterceptorMethods(List<Method> interceptorMethods);

	String getUriPattern();

	void setUriPattern(String uriPattern);

	String getView();

	void setView(String view);

	Integer getOrder();

	void setOrder(Integer order);
}
