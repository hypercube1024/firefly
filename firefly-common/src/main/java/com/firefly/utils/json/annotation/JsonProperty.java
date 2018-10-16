package com.firefly.utils.json.annotation;

import java.lang.annotation.*;

/**
 * @author Pengtao Qiu
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonProperty {
    String value() default "";
}
