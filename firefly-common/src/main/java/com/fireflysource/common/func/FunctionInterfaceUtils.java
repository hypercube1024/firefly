package com.fireflysource.common.func;

import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
abstract public class FunctionInterfaceUtils {

    public static final Callback NOOP_CALLBACK = () -> {
    };


    public static <T> Consumer<T> createEmptyConsumer() {
        return t -> {
        };
    }
}
