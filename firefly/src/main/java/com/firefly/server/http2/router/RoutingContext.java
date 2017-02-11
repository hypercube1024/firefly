package com.firefly.server.http2.router;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;

import javax.servlet.http.Part;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pengtao Qiu
 */
public interface RoutingContext extends Cloneable {

    Object get(String key);

    Object put(String key, Object value);

    Object remove(String key);

    ConcurrentHashMap<String, Object> getAttributes();

    SimpleResponse getResponse();

    SimpleRequest getRequest();

    String getPathParameter(String name);

    String getParameter(String name);

    List<String> getParameterValues(String name);

    Map<String, List<String>> getParameterMap();

    Collection<Part> getParts();

    Part getPart(String name);

    void next();
}
