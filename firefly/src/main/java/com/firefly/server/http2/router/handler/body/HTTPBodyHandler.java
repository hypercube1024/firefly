package com.firefly.server.http2.router.handler.body;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.*;
import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.impl.RoutingContextImpl;
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
import java.util.concurrent.atomic.AtomicLong;

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
    public void handle(RoutingContext context) {
        RoutingContextImpl ctx = (RoutingContextImpl) context;
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

        if (isChunked(request)) {
            httpBodyHandlerSPI.pipedStream = new ByteArrayPipedStream(4 * 1024);
        } else {
            long contentLength = request.getContentLength();
            if (contentLength <= 0) { // no content
                ctx.next();
                return;
            } else {
                if (contentLength > configuration.getBodyBufferThreshold()) {
                    httpBodyHandlerSPI.pipedStream = new FilePipedStream(configuration.getTempFilePath());
                } else {
                    httpBodyHandlerSPI.pipedStream = new ByteArrayPipedStream((int) contentLength);
                }
            }
        }

        AtomicLong chunkedEncodingContentLength = new AtomicLong();
        ctx.content(buf -> {
            if (log.isDebugEnabled()) {
                log.debug("http body handler received content size -> {}", buf.remaining());
            }

            try {
                if (isChunked(request)) {
                    if (chunkedEncodingContentLength.addAndGet(buf.remaining()) > configuration.getBodyBufferThreshold()
                            && httpBodyHandlerSPI.pipedStream instanceof ByteArrayPipedStream) {
                        // chunked encoding content dump to temp file
                        IO.close(httpBodyHandlerSPI.pipedStream.getOutputStream());
                        FilePipedStream filePipedStream = new FilePipedStream(configuration.getTempFilePath());
                        IO.copy(httpBodyHandlerSPI.pipedStream.getInputStream(), filePipedStream.getOutputStream());
                        filePipedStream.getOutputStream().write(BufferUtils.toArray(buf));
                        httpBodyHandlerSPI.pipedStream = filePipedStream;
                    } else {
                        httpBodyHandlerSPI.pipedStream.getOutputStream().write(BufferUtils.toArray(buf));
                    }
                } else {
                    httpBodyHandlerSPI.pipedStream.getOutputStream().write(BufferUtils.toArray(buf));
                }
            } catch (IOException e) {
                log.error("http server receives http body exception", e);
            }
        }).contentComplete(req -> {
            try {
                String contentType = MimeTypes.getContentTypeMIMEType(request.getFields().get(HttpHeader.CONTENT_TYPE));
                httpBodyHandlerSPI.pipedStream.getOutputStream().close();
                if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                    try (InputStream inputStream = httpBodyHandlerSPI.pipedStream.getInputStream()) {
                        httpBodyHandlerSPI.urlEncodedMap.decode(IO.toString(inputStream, configuration.getCharset()),
                                Charset.forName(configuration.getCharset()));
                    }
                } else if ("multipart/form-data".equalsIgnoreCase(contentType)) {
                    httpBodyHandlerSPI.multiPartFormInputStream = new MultiPartFormInputStream(
                            httpBodyHandlerSPI.getInputStream(),
                            request.getFields().get(HttpHeader.CONTENT_TYPE),
                            configuration.getMultipartConfigElement(),
                            new File(configuration.getTempFilePath()));
                }
            } catch (IOException e) {
                log.error("http server ends receiving data exception", e);
            }
        }).messageComplete(req -> ctx.next());
    }

    public boolean isChunked(SimpleRequest request) {
        String transferEncoding = request.getFields().get(HttpHeader.TRANSFER_ENCODING);
        return HttpHeaderValue.CHUNKED.asString().equals(transferEncoding)
                || (request.getHttpVersion() == HttpVersion.HTTP_2 && request.getContentLength() < 0);
    }
}
