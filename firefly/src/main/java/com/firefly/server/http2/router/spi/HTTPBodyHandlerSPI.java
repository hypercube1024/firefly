package com.firefly.server.http2.router.spi;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.utils.function.Action1;

import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.ByteBuffer;
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

    void content(Action1<ByteBuffer> content);

    void contentComplete(Action1<SimpleRequest> contentComplete);
}
