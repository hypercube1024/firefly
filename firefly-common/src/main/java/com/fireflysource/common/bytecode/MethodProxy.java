package com.fireflysource.common.bytecode;

import java.lang.reflect.Method;

/**
 * @author Pengtao Qiu
 */
public interface MethodProxy {
    Method method();

    /**
     * Executes this method
     *
     * @param obj  The instance of object that contains this method
     * @param args The parameters of this method
     * @return Return value of this method
     */
    Object invoke(Object obj, Object... args);
}
