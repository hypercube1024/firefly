package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.firefly.annotation.RequestMapping;

public class ControllerMetaInfo extends HandlerMetaInfo {

	private final Set<String> allowHttpMethod;

	public ControllerMetaInfo(Object object, Method method) {
		super(object, method);
		allowHttpMethod = new HashSet<String>(Arrays.asList(method.getAnnotation(RequestMapping.class).method()));
	}

	public boolean allowMethod(String method) {
		return allowHttpMethod.contains(method);
	}

	public String getAllowMethod() {
		StringBuilder s = new StringBuilder();
		for (String m : allowHttpMethod) {
			s.append(m).append(',');
		}
		s.deleteCharAt(s.length() - 1);
		return s.toString();
	}

	@Override
	public String toString() {
		return "ControllerMetaInfo [allowHttpMethod=" + allowHttpMethod + "]";
	}

}
