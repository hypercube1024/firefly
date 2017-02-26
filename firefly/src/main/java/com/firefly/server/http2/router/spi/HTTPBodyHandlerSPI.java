package com.firefly.server.http2.router.spi;

import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;

import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface HTTPBodyHandlerSPI {

    String getParameter(String name);

    List<String> getParameterValues(String name);

    Map<String, List<String>> getParameterMap();

    Collection<Part> getParts();

    Part getPart(String name);

    InputStream getInputStream();

    BufferedReader getBufferedReader();

    String getStringBody(String charset);

    String getStringBody();

    <T> T getJsonBody(Class<T> clazz);

    JsonObject getJsonObjectBody();

    JsonArray getJsonArrayBody();

}
