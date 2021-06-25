package com.fireflysource.common.bytecode;

public interface ClassProxyFactory {
    <T> T createProxy(T instance, ClassProxy proxy, MethodFilter methodFilter) throws Throwable;
}
