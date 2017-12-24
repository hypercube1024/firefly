package com.firefly.net.tcp.codec.ffsocks.stream;

import com.firefly.net.tcp.codec.ffsocks.model.Request;
import com.firefly.net.tcp.codec.ffsocks.model.Response;

import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksContext {

    Request getRequest();

    Response getResponse();

    byte[] getData();

    Map<String, Object> getAttibutes();

    void setAttribute(String key, Object value);

    Object getAttribute(String key);

}
