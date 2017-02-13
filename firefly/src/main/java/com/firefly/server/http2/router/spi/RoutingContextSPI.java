package com.firefly.server.http2.router.spi;

import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface RoutingContextSPI {

    String getParameter(String name);

    List<String> getParameterValues(String name);

    Map<String, List<String>> getParameterMap();

    Collection<Part> getParts();

    Part getPart(String name);

    HttpSession getHttpSession();

}
