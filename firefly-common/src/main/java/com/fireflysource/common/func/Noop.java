package com.fireflysource.common.func;

import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public class Noop {

    public static final Callback NOOP_CALLBACK = () -> {
    };

    public static final Consumer NOOP_CONSUMER = o -> {
    };

}
