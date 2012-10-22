package com.firefly.mvc.web.support;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.firefly.mvc.web.View;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class HandlerMetaInfo {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected final Object object; // controller的实例对象
	protected final Method method; // 请求uri对应的方法
	protected final byte[] methodParam; // 请求方法参数类型
	
	public HandlerMetaInfo(Object object, Method method) {
		this.object = object;
		this.method = method;
		this.methodParam = new byte[method.getParameterTypes().length];
	}

	public byte[] getMethodParam() {
		return methodParam;
	}
	
	public View invoke(Object[] args) {
		View ret = null;
		try {
			ret = (View)method.invoke(object, args);
		} catch (Throwable t) {
			log.error("controller invoke error", t);
		}
		return ret;
	}

	@Override
	public String toString() {
		return "HandlerMetaInfo [method=" + method + ", methodParam="
				+ Arrays.toString(methodParam) + "]";
	}

}
