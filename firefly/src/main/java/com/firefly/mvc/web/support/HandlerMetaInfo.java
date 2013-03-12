package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.firefly.mvc.web.View;
import com.firefly.mvc.web.support.exception.WebException;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ProxyMethod;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class HandlerMetaInfo {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected final Object object; // controller的实例对象
	protected final ProxyMethod proxy; // 请求uri对应的方法
	protected final byte[] methodParam; // 请求方法参数类型
	
	public HandlerMetaInfo(Object object, Method method) {
		this.object = object;
		try {
			this.proxy = ReflectUtils.getProxyMethod(method);
		} catch (Throwable e) {
			log.error("handler init error", e);
			throw new WebException("handler invoke error");
		}
		this.methodParam = new byte[method.getParameterTypes().length];
	}

	public byte[] getMethodParam() {
		return methodParam;
	}
	
	public final View invoke(Object[] args) {
		return (View)proxy.invoke(object, args);
	}

	@Override
	public String toString() {
		return "HandlerMetaInfo [method=" + proxy.method() + ", methodParam=" + Arrays.toString(methodParam) + "]";
	}

}
