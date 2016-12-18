package com.firefly.template2.generator;

import java.io.Writer;

/**
 * @author Pengtao Qiu
 */
public interface JavaGenerator<T> {

    void enter(T node, Writer writer, Object... args);

    void exit(T node, Writer writer, Object... args);

}
