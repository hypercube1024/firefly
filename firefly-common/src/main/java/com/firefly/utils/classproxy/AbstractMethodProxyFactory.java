package com.firefly.utils.classproxy;

import java.lang.reflect.Field;

import com.firefly.utils.ReflectUtils.ArrayProxy;
import com.firefly.utils.ReflectUtils.FieldProxy;

abstract public class AbstractMethodProxyFactory extends AbstractProxyFactory {

	@Override
	public ArrayProxy getArrayProxy(Class<?> clazz) throws Throwable {
		return null;
	}

	@Override
	public FieldProxy getFieldProxy(Field field) throws Throwable {
		return null;
	}

}
