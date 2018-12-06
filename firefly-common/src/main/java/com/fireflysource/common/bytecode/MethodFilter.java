package com.fireflysource.common.bytecode;

import java.lang.reflect.Method;

public interface MethodFilter {
    boolean accept(Method method);
}
