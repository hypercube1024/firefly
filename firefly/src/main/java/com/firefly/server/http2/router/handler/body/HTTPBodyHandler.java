package com.firefly.server.http2.router.handler.body;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.codec.http2.model.MultiPartInputStreamParser;
import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.StringUtils;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.ByteArrayPipedStream;
import com.firefly.utils.io.FilePipedStream;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author Pengtao Qiu
 */
public class HTTPBodyHandler implements Handler {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private HTTPBodyConfiguration configuration;

    public HTTPBodyHandler() {
        this(new HTTPBodyConfiguration());
    }

    public HTTPBodyHandler(HTTPBodyConfiguration configuration) {
        this.configuration = configuration;
    }

    public HTTPBodyConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(HTTPBodyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void handle(RoutingContext ctx) {
        SimpleRequest request = ctx.getRequest();
        HTTPBodyHandlerSPIImpl httpBodyHandlerSPI = new HTTPBodyHandlerSPIImpl();
        httpBodyHandlerSPI.urlEncodedMap = new UrlEncoded();
        httpBodyHandlerSPI.charset = configuration.getCharset();
        ctx.setHTTPBodyHandlerSPI(httpBodyHandlerSPI);

        if (StringUtils.hasText(request.getURI().getQuery())) {
            httpBodyHandlerSPI.urlEncodedMap.decode(request.getURI().getQuery(), Charset.forName(configuration.getCharset()));
        }

        if (ctx.isAsynchronousRead()) { // receive content event has been listened
            ctx.next();
            return;
        }

        long contentLength = request.getContentLength();
        if (contentLength <= 0) { // no content
            ctx.next();
            return;
        }

        if (contentLength > configuration.getBodyBufferThreshold()) {
            httpBodyHandlerSPI.pipedStream = new FilePipedStream(configuration.getTempFilePath());
        } else {
            httpBodyHandlerSPI.pipedStream = new ByteArrayPipedStream((int) contentLength);
        }


        String contentType = MimeTypes.getContentTypeMIMEType(request.getFields().get(HttpHeader.CONTENT_TYPE));
        if ("multipart/form-data".equals(contentType)) {
            httpBodyHandlerSPI.multiPartInputStreamParser = new MultiPartInputStreamParser(
                    httpBodyHandlerSPI.getInputStream(),
                    request.getFields().get(HttpHeader.CONTENT_TYPE),
                    configuration.getMultipartConfigElement(),
                    new File(configuration.getTempFilePath()));
        }

        ctx.content(buf -> {
            try {
                httpBodyHandlerSPI.pipedStream.getOutputStream().write(BufferUtils.toArray(buf));
            } catch (IOException e) {
                log.error("http server receives http body exception", e);
            }
        }).contentComplete(req -> {
            try {
                httpBodyHandlerSPI.pipedStream.getOutputStream().close();
                if ("application/x-www-form-urlencoded".equals(contentType)) {
                    try (InputStream inputStream = httpBodyHandlerSPI.pipedStream.getInputStream()) {
                        httpBodyHandlerSPI.urlEncodedMap.decode(IO.toString(inputStream, configuration.getCharset()),
                                Charset.forName(configuration.getCharset()));
                    }
                }
            } catch (IOException e) {
                log.error("http server ends receiving data exception", e);
            }
        }).messageComplete(req -> ctx.next());
    }

}
