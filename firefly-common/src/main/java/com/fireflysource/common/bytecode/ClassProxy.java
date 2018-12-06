package com.fireflysource.common.bytecode;

public interface ClassProxy {
    Object intercept(MethodProxy handler, Object originalInstance, Object[] args);
}
