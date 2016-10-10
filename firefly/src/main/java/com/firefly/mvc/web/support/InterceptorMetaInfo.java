package com.firefly.mvc.web.support;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.mvc.web.HandlerChain;
import com.firefly.utils.pattern.Pattern;

public class InterceptorMetaInfo extends HandlerMetaInfo implements Comparable<InterceptorMetaInfo> {

	private final Pattern pattern;
	private final Integer order;

	public InterceptorMetaInfo(Object object, Method method, String uriPattern, int order) {
		super(object, method);
		Class<?>[] paraTypes = method.getParameterTypes();

		for (int i = 0; i < paraTypes.length; i++) {
			if (paraTypes[i].equals(HttpServletRequest.class)) {
				methodParam[i] = MethodParam.REQUEST;
			} else if (paraTypes[i].equals(HttpServletResponse.class)) {
				methodParam[i] = MethodParam.RESPONSE;
			} else if (paraTypes[i].equals(HandlerChain.class)) {
				methodParam[i] = MethodParam.HANDLER_CHAIN;
			}
		}

		pattern = Pattern.compile(uriPattern, "*");
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
