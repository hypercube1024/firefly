package com.firefly.client.http2;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.AbstractHTTP2OutputStream;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTP2ClientResponseHandler extends Stream.Listener.Adapter {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final Request request;
    private final ClientHTTPHandler handler;
    private final HTTPClientConnection connection;

    public HTTP2ClientResponseHandler(Request request, ClientHTTPHandler handler, HTTPClientConnection connection) {
        this.request = request;
        this.handler = handler;
        this.connection = connection;
    }

    @Override
    public void onHeaders(final Stream stream, final HeadersFrame headersFrame) {
        System.out.println("on headers ," + stream + ", " + headersFrame);
        if (headersFrame.getMetaData() == null) {
            throw new IllegalArgumentException("the stream " + stream.getId() + " received a null meta data");
        }

        if (headersFrame.getMetaData().isResponse()) {
            final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
            final MetaData.Response response = (MetaData.Response) headersFrame.getMetaData();

            if (response.getStatus() == 100) {
                handler.continueToSendData(request, response, output, connection);
            } else {
                stream.setAttribute("response", response);
                handler.headerComplete(request, response, output, connection);
                if (headersFrame.isEndStream()) {
                    handler.messageComplete(request, response, output, connection);
                }
            }
        } else {
            if (headersFrame.isEndStream()) {
                final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
                final MetaData.Response response = (MetaData.Response) stream.getAttribute("response");
                response.setTrailerSupplier(() -> headersFrame.getMetaData().getFields());
                handler.contentComplete(request, response, output, connection);
                handler.messageComplete(request, response, output, connection);
            } else {
                throw new IllegalArgumentException("the stream " + stream.getId() + " received illegal meta data");
            }
        }

    }

    @Override
    public void onData(Stream stream, DataFrame dataFrame, Callback callback) {
        final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
        final MetaData.Response response = (MetaData.Response) stream.getAttribute("response");

        try {
            handler.content(dataFrame.getData(), request, response, output, connection);
            callback.succeeded();
        } catch (Throwable t) {
            callback.failed(t);
        }

        if (dataFrame.isEndStream()) {
            handler.contentComplete(request, response, output, connection);
            handler.messageComplete(request, response, output, connection);
        }
    }

    @Override
    public void onReset(Stream stream, ResetFrame frame) {
        final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
        final MetaData.Response response = (MetaData.Response) stream.getAttribute("response");

        ErrorCode errorCode = ErrorCode.from(frame.getError());
        String reason = errorCode == null ? "error=" + frame.getError() : errorCode.name().toLowerCase();
        int status = HttpStatus.INTERNAL_SERVER_ERROR_500;

        if (errorCode != null) {
            switch (errorCode) {
                case PROTOCOL_ERROR:
                    status = HttpStatus.BAD_REQUEST_400;
                    break;
                default:
                    status = HttpStatus.INTERNAL_SERVER_ERROR_500;
                    break;
            }
        }
        handler.badMessage(status, reason, request, response, output, connection);
    }

    public static class ClientHttp2OutputStream extends AbstractHTTP2OutputStream {

        private final Stream stream;

        public ClientHttp2OutputStream(MetaData info, boolean endStream, Stream stream) {
            super(info, true);
            committed = true;
            this.stream = stream;
            if (endStream) {
                isChunked = false;
            } else {
                if (info.getFields().contains(HttpHeader.CONTENT_LENGTH)) {
                    if (log.isDebugEnabled()) {
                        log.debug("stream {} commits header that contains content length {}", getStream().getId(),
                                info.getFields().get(HttpHeader.CONTENT_LENGTH));
                    }
                    isChunked = false;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("stream {} commits header using chunked encoding", getStream().getId());
                    }
                    isChunked = true;
                }
            }
        }

        @Override
        protected Stream getStream() {
            return stream;
        }
    }

    public static class ClientStreamPromise implements Promise<Stream> {

        private final Request request;
        private final Promise<HTTPOutputStream> promise;
        private final boolean endStream;

        public ClientStreamPromise(Request request, Promise<HTTPOutputStream> promise, boolean endStream) {
            this.request = request;
            this.promise = promise;
            this.endStream = endStream;
        }

        @Override
        public void succeeded(final Stream stream) {
            if (log.isDebugEnabled()) {
                log.debug("create a new stream {}", stream.getId());
            }
            final AbstractHTTP2OutputStream output = new ClientHttp2OutputStream(request, endStream, stream);
            stream.setAttribute("outputStream", output);
            promise.succeeded(output);
        }

        @Override
        public void failed(Throwable x) {
            promise.failed(x);
            log.error("client creates stream unsuccessfully", x);
        }

    }
}
