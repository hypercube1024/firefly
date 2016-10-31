package com.firefly.utils.json.support;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.MethodProxy;

import java.lang.reflect.Method;

public class MethodInvoke implements PropertyInvoke {

	private MethodProxy method;

	public MethodInvoke(Method method) {
		try {
			this.method = ReflectUtils.getMethodProxy(method);
		} catch (Throwable e) {
		}
	}

	@Override
	public void set(Object obj, Object arg) {
		method.invoke(obj, arg);
	}

	@Override
	public Object get(Object obj) {
		return method.invoke(obj);
	}

}
