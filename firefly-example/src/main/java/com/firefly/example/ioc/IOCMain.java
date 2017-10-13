package com.firefly.example.ioc;

import com.firefly.$;
import com.firefly.core.ApplicationContext;

/**
 * @author Pengtao Qiu
 */
public class IOCMain {
    public static final ApplicationContext ctx = $.createApplicationContext("hello-ioc.xml");

    public static void main(String[] args) {
        ctx.getBean(FooService.class).say("foo");
    }
}
