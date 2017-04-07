package com.firefly.utils.classproxy;

public interface ClassProxyFactory {
    Object createProxy(Object instance, ClassProxy proxy, MethodFilter method) throws Throwable;
}
