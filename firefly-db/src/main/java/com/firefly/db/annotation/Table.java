package com.firefly.db.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    String value();

    String catalog() default "";

    String schema() default "";
}
