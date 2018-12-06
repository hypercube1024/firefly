package com.fireflysource.common.bytecode;

import java.lang.reflect.Field;

/**
 * @author Pengtao Qiu
 */
public interface FieldProxy {
    Field field();

    Object get(Object obj);

    void set(Object obj, Object value);
}
