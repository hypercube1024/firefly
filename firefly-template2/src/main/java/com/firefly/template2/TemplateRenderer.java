package com.firefly.template2;

import com.firefly.template2.model.VariableStorage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Pengtao Qiu
 */
public interface TemplateRenderer {

    void main(OutputStream outputStream, VariableStorage var) throws IOException;

}
