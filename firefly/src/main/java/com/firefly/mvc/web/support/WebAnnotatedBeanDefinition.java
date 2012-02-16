package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.List;

import com.firefly.core.support.annotation.AnnotatedBeanDefinition;

public class WebAnnotatedBeanDefinition extends AnnotatedBeanDefinition
		implements WebBeanDefinition {

	private List<Method> reqMethods, interceptorMethods;
	private String uriPattern, view;
	private Integer order;

	@Override
	public List<Method> getInterceptorMethods() {
		return interceptorMethods;
	}

	@Override
	public List<Method> getReqMethods() {
		return reqMethods;
	}

	@Override
	public void setInterceptorMethods(List<Method> interceptorMethods) {
		this.interceptorMethods = interceptorMethods;
	}

	@Override
	public void setReqMethods(List<Method> reqMethods) {
		this.reqMethods = reqMethods;
	}

	@Override
	public String getUriPattern() {
		return uriPattern;
	}

	@Override
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	@Override
	public String getView() {
		return view;
	}

	@Override
	public void setView(String view) {
		this.view = view;
	}

	@Override
	public Integer getOrder() {
		return order;
	}

	@Override
	public void setOrder(Integer order) {
		this.order = order;
	}

}
