//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package com.firefly.utils.lang.annotation;

import java.lang.annotation.*;

/**
 * This annotation is used to describe variables in method
 * signatures so that when rendered into tools like JConsole
 * it is clear what the parameters are. For example:
 * <p>
 * public void doodle(@Name(value="doodle", description="A description of the argument") String doodle)
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.PARAMETER})
public @interface Name {
    /**
     * the name of the parameter
     *
     * @return the value
     */
    String value();

    /**
     * the description of the parameter
     *
     * @return the description
     */
    String description() default "";
}
