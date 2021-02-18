package com.fireflysource.common.bytecode;

/**
 * @author Pengtao Qiu
 */
public interface ArrayProxy {
    int size(Object array);

    Object get(Object array, int index);

    void set(Object array, int index, Object value);
}
