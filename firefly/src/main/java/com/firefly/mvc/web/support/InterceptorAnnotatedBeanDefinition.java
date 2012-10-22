package com.firefly.mvc.web.support;

import java.lang.reflect.Method;

import com.firefly.core.support.annotation.AnnotatedBeanDefinition;

public class InterceptorAnnotatedBeanDefinition extends AnnotatedBeanDefinition implements InterceptorBeanDefinition{

	private Method disposeMethod;
	private String uriPattern;
	private int order;
	
	@Override
	public String getUriPattern() {
		return uriPattern;
	}

	@Override
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public Method getDisposeMethod() {
		return disposeMethod;
	}

	@Override
	public void setDisposeMethod(Method method) {
		disposeMethod = method;
	}

}
