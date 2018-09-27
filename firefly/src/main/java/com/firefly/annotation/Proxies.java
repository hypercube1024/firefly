package com.firefly.annotation;

import java.lang.annotation.*;

/**
 * @author Pengtao Qiu
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Proxies {
    Proxy[] value();
}
