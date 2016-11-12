package com.firefly.annotation;

import java.lang.annotation.*;

/**
 * The multipart settings for method
 *
 * @author Pengtao Qiu
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultipartSettings {
    /**
     * The directory location where files will be stored
     */
    String location() default "";

    /**
     * The maximum size allowed for uploaded files.
     * <p>
     * <p>The default is <tt>-1L</tt>, which means unlimited.
     */
    long maxFileSize() default -1L;

    /**
     * The maximum size allowed for <tt>multipart/form-data</tt>
     * requests
     * <p>
     * <p>The default is <tt>-1L</tt>, which means unlimited.
     */
    long maxRequestSize() default -1L;

    /**
     * The size threshold after which the file will be written to disk
     */
    int fileSizeThreshold() default 0;
}
