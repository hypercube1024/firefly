package com.firefly.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {
    String value() default "";
}
