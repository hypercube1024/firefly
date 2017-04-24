package com.firefly.utils.classproxy;

public interface ClassProxyFactory {
    <T> T createProxy(T instance, ClassProxy proxy, MethodFilter method) throws Throwable;
}
