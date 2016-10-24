package com.firefly.mvc.web.support;

import java.lang.reflect.Method;

import com.firefly.utils.pattern.Pattern;

public class InterceptorMetaInfo extends HandlerMetaInfo implements Comparable<InterceptorMetaInfo> {

	private final Pattern pattern;
	private final Integer order;

	public InterceptorMetaInfo(Object object, Method method, String uriPattern, int order) {
		super(object, method);
		this.pattern = Pattern.compile(uriPattern, "*");
		this.order = order;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public Integer getOrder() {
		return order;
	}

	@Override
	public int compareTo(InterceptorMetaInfo o) {
		return order.compareTo(o.order);
	}

}
