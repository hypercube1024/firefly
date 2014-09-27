package com.firefly.utils.classproxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.firefly.utils.ReflectUtils.FieldProxy;
import com.firefly.utils.ReflectUtils.MethodProxy;

abstract public class AbstractArrayProxyFactory extends AbstractProxyFactory {

	@Override
	public FieldProxy getFieldProxy(Field field) throws Throwable {
		return null;
	}
	
	@Override
	public MethodProxy getMethodProxy(Method method) throws Throwable {
		return null;
	}
}
