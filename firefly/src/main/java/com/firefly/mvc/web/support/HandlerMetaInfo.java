package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.exception.WebException;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class HandlerMetaInfo {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	protected final Object object; // controller or interceptor instance
	protected final MethodProxy proxy; // mapped method of the URI
	protected final MethodParam[] methodParam; // method type

	public HandlerMetaInfo(Object object, Method method) {
		this.object = object;
		try {
			this.proxy = ReflectUtils.getMethodProxy(method);
		} catch (Throwable e) {
			log.error("handler init error", e);
			throw new WebException("handler invoke error");
		}
		this.methodParam = new MethodParam[method.getParameterTypes().length];
	}

	public MethodParam[] getMethodParam() {
		return methodParam;
	}

	public final View invoke(Object[] args) {
		return (View) proxy.invoke(object, args);
	}

	@Override
	public String toString() {
		return "HandlerMetaInfo [method=" + proxy.method() + ", methodParam=" + Arrays.toString(methodParam) + "]";
	}

}
