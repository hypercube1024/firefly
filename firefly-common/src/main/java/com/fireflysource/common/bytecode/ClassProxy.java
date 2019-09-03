package com.fireflysource.common.bytecode;

@FunctionalInterface
public interface ClassProxy {
    Object intercept(MethodProxy handler, Object originalInstance, Object[] args);
}
