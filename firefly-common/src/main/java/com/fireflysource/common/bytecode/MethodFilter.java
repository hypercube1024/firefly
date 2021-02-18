package com.fireflysource.common.bytecode;

import java.lang.reflect.Method;

@FunctionalInterface
public interface MethodFilter {
    boolean accept(Method method);
}
