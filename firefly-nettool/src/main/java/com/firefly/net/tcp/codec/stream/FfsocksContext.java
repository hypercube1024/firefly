package com.firefly.net.tcp.codec.stream;

import com.firefly.net.tcp.codec.model.Request;
import com.firefly.net.tcp.codec.model.Response;

import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksContext {
    Request getRequest();

    Response getResponse();

    <T> T getData();

    Map<String, Object> getAttibutes();

    void setAttribute(String key, Object value);

    Object getAttribute(String key);
}
