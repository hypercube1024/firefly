package com.firefly.net.tcp.codec.flex.stream;

import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface ContextAttribute {

    Map<String, Object> getAttributes();

    void setAttribute(String key, Object value);

    Object getAttribute(String key);
}
