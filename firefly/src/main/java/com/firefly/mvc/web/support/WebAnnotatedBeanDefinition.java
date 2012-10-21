package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.List;

import com.firefly.core.support.annotation.AnnotatedBeanDefinition;

public class WebAnnotatedBeanDefinition extends AnnotatedBeanDefinition
		implements WebBeanDefinition {

	private List<Method> reqMethods;


	@Override
	public List<Method> getReqMethods() {
		return reqMethods;
	}
	
	@Override
	public void setReqMethods(List<Method> reqMethods) {
		this.reqMethods = reqMethods;
	}

}
