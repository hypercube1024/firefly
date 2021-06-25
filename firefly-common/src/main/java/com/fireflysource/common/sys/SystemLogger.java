package com.fireflysource.common.sys;

import com.fireflysource.common.bytecode.JavassistClassProxyFactory;
import com.fireflysource.common.slf4j.LazyLogger;
import org.slf4j.MDC;

/**
 * @author Pengtao Qiu
 */
public class SystemLogger {

    private static final LazyLogger system = LazyLogger.create("firefly-system");

    public static LazyLogger create(Class<?> clazz) {
        try {
            String className = clazz.getSimpleName();
            return JavassistClassProxyFactory.INSTANCE.createProxy(system, ((handler, originalInstance, args) -> {
                try (MDC.MDCCloseable mdc = MDC.putCloseable("class", className)) {
                    return handler.invoke(originalInstance, args);
                }
            }), null);
        } catch (Throwable e) {
            system.error("create system logger exception", e);
            throw new IllegalStateException(e);
        }
    }

}
