package com.firefly.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.firefly.mvc.web.View;

@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Interceptor {
	String value() default "";

	String uri();

	String view() default View.JSP;

	int order() default 0;

}
