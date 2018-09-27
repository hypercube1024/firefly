package com.firefly.utils.json.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DateFormat {

    String value() default "yyyy-MM-dd HH:mm:ss";

    DateFormatType type() default DateFormatType.DATE_PATTERN_STRING;
}
