package com.firefly.utils.json.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvoke implements PropertyInvoke {

	private Method method;

	public MethodInvoke(Method method) {
		this.method = method;
	}

	@Override
	public void set(Object obj, Object arg) {
		try {
			method.invoke(obj, arg);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object get(Object obj) {
		Object ret = null;
		try {
			ret = method.invoke(obj);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return ret;
	}

}
