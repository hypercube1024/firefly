package com.firefly.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Proxies.class)
@Documented
public @interface Proxy {
    Class<?> proxyClass();
}
