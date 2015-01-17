package com.firefly.utils.classproxy;

public interface ClassProxyFactory {
	public Object createProxy(Object instance, ClassProxy proxy, MethodFilter method) throws Throwable;
}
