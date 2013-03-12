package com.firefly.utils.json.support;

import java.lang.reflect.Method;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ProxyMethod;

public class MethodInvoke implements PropertyInvoke {

	private ProxyMethod method;

	public MethodInvoke(Method method) {
		try {
			this.method = ReflectUtils.getProxyMethod(method);
		} catch (Throwable e) {
			e.printStackTrace();
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
