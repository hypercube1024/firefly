package com.firefly.server.http2.router.handler.body;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.MultiPartInputStreamParser;
import com.firefly.server.http2.router.spi.HTTPBodyHandlerSPI;
import com.firefly.utils.io.IO;
import com.firefly.utils.io.PipedStream;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.GenericTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class HTTPBodyHandlerSPIImpl implements HTTPBodyHandlerSPI {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    PipedStream pipedStream;
    MultiPartInputStreamParser multiPartInputStreamParser;
    UrlEncoded urlEncodedMap;
    String charset;
    private BufferedReader bufferedReader;
    private String stringBody;


    @Override
    public String getParameter(String name) {
        return urlEncodedMap.getString(name);
    }

    @Override
    public List<String> getParameterValues(String name) {
        return urlEncodedMap.getValues(name);
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return urlEncodedMap;
    }

    @Override
    public Collection<Part> getParts() {
        if (multiPartInputStreamParser == null) {
            return null;
        } else {
            try {
                return multiPartInputStreamParser.getParts();
            } catch (IOException e) {
                log.error("get multi part exception", e);
                return null;
            }
        }
    }

    @Override
    public Part getPart(String name) {
        if (multiPartInputStreamParser == null) {
            return null;
        } else {
            try {
                return multiPartInputStreamParser.getPart(name);
            } catch (IOException e) {
                log.error("get multi part exception", e);
                return null;
            }
        }
    }

    @Override
    public InputStream getInputStream() {
        if (pipedStream == null) {
            return null;
        } else {
            try {
                return pipedStream.getInputStream();
            } catch (IOException e) {
                log.error("get input stream exception", e);
                return null;
            }
        }
    }

    @Override
    public BufferedReader getBufferedReader() {
        if (bufferedReader != null) {
            return bufferedReader;
        } else {
            if (pipedStream == null) {
                return null;
            } else {
                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(pipedStream.getInputStream()));
                    return bufferedReader;
                } catch (IOException e) {
                    log.error("get buffered reader exception", e);
                    return null;
                }
            }
        }
    }

    @Override
    public String getStringBody(String charset) {
        if (stringBody != null) {
            return stringBody;
        } else {
            if (getInputStream() == null) {
                return null;
            } else {
                try (InputStream inputStream = getInputStream()) {
                    stringBody = IO.toString(inputStream, Charset.forName(charset));
                    return stringBody;
                } catch (IOException e) {
                    log.error("get string body exception", e);
                    return null;
                }
            }
        }
    }

    @Override
    public String getStringBody() {
        return getStringBody(charset);
    }

    @Override
    public <T> T getJsonBody(Class<T> clazz) {
        return Json.toObject(getStringBody(), clazz);
    }

    @Override
    public <T> T getJsonBody(GenericTypeReference<T> typeReference) {
        return Json.toObject(getStringBody(),typeReference);
    }

    @Override
    public JsonObject getJsonObjectBody() {
        return Json.toJsonObject(getStringBody());
    }

    @Override
    public JsonArray getJsonArrayBody() {
        return Json.toJsonArray(getStringBody());
    }

}
