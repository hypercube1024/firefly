package com.firefly.template2;

import com.firefly.template2.model.VariableStorage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author Pengtao Qiu
 */
public interface TemplateRenderer {

    void main(OutputStream outputStream, VariableStorage var) throws IOException;

    default byte[] toBytes(String string, String charset) {
        try {
            return string.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            return string.getBytes();
        }
    }
}
