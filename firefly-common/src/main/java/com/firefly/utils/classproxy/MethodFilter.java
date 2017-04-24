package com.firefly.utils.classproxy;

import java.lang.reflect.Method;

public interface MethodFilter {
    boolean accept(Method method);
}
