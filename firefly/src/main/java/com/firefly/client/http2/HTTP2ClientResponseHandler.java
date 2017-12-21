package com.firefly.client.http2;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.AbstractHTTP2OutputStream;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTP2ClientResponseHandler extends Stream.Listener.Adapter {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public static final String OUTPUT_STREAM_KEY = "_outputStream";
    public static final String RESPONSE_KEY = "_response";

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
        // TODO wait local stream creating success.
         System.out.println("Client received header: " + stream.toString() + ", " + headersFrame.toString());
        if (headersFrame.getMetaData() == null) {
            // System.out.println("Client received meta data is null");
            throw new IllegalArgumentException("the stream " + stream.getId() + " received a null meta data");
        }

        if (headersFrame.getMetaData().isResponse()) {
            final HTTPOutputStream output = getOutputStream(stream);
            final MetaData.Response response = (MetaData.Response) headersFrame.getMetaData();

            if (response.getStatus() == HttpStatus.CONTINUE_100) {
                handler.continueToSendData(request, response, output, connection);
            } else {
                stream.setAttribute(RESPONSE_KEY, response);
                handler.headerComplete(request, response, output, connection);
                if (headersFrame.isEndStream()) {
                    handler.messageComplete(request, response, output, connection);
                }
            }
        } else {
            if (headersFrame.isEndStream()) {
                final HTTPOutputStream output = getOutputStream(stream);
                final MetaData.Response response = getResponse(stream);
                response.setTrailerSupplier(() -> headersFrame.getMetaData().getFields());
                handler.contentComplete(request, response, output, connection);
                handler.messageComplete(request, response, output, connection);
            } else {
                throw new IllegalArgumentException("the stream " + stream.getId() + " received illegal meta data");
            }
        }

    }

    private HTTPOutputStream getOutputStream(Stream stream) {
        return (HTTPOutputStream) stream.getAttribute(OUTPUT_STREAM_KEY);
    }

    @Override
    public void onData(Stream stream, DataFrame dataFrame, Callback callback) {
        System.out.println("Client received: " + stream + ", " + dataFrame);
        final HTTPOutputStream output = getOutputStream(stream);
        final MetaData.Response response = getResponse(stream);

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

    private MetaData.Response getResponse(Stream stream) {
        return (MetaData.Response) stream.getAttribute(RESPONSE_KEY);
    }

    @Override
    public void onReset(Stream stream, ResetFrame frame) {
        // System.out.println("Client received reset frame: " + stream + ", " + frame);
        final HTTPOutputStream output = getOutputStream(stream);
        final MetaData.Response response = getResponse(stream);

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

        public ClientHttp2OutputStream(MetaData info, Stream stream) {
            super(info, true);
            committed = true;
            this.stream = stream;
        }

        @Override
        protected Stream getStream() {
            return stream;
        }
    }

    public static class ClientStreamPromise implements Promise<Stream> {

        private final Request request;
        private final Promise<HTTPOutputStream> promise;

        public ClientStreamPromise(Request request, Promise<HTTPOutputStream> promise) {
            this.request = request;
            this.promise = promise;
        }

        @Override
        public void succeeded(final Stream stream) {
            if (log.isDebugEnabled()) {
                log.debug("create a new stream {}", stream.getId());
            }

            ClientHttp2OutputStream output = new ClientHttp2OutputStream(request, stream);
            stream.setAttribute(OUTPUT_STREAM_KEY, output);
            promise.succeeded(output);
        }

        @Override
        public void failed(Throwable x) {
            promise.failed(x);
            log.error("client creates stream unsuccessfully", x);
        }

    }
}
