package com.firefly.template2.generator;

import java.io.Writer;

/**
 * @author Pengtao Qiu
 */
public interface JavaGenerator<T> {
    void generate(T node, Writer writer);
}
