package com.firefly.client.http2;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPHandler;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.function.Action4;
import com.firefly.utils.function.Action6;
import com.firefly.utils.function.Func4;
import com.firefly.utils.function.Func5;

import java.nio.ByteBuffer;

public interface ClientHTTPHandler extends HTTPHandler {

    void continueToSendData(MetaData.Request request, MetaData.Response response, HTTPOutputStream output,
                            HTTPConnection connection);

    class Adapter extends HTTPHandler.Adapter implements ClientHTTPHandler {

        protected Action4<Request, Response, HTTPOutputStream, HTTPConnection> continueToSendData;

        public ClientHTTPHandler.Adapter headerComplete(
                Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> headerComplete) {
            this.headerComplete = headerComplete;
            return this;
        }

        public ClientHTTPHandler.Adapter content(
                Func5<ByteBuffer, Request, Response, HTTPOutputStream, HTTPConnection, Boolean> content) {
            this.content = content;
            return this;
        }

        public ClientHTTPHandler.Adapter contentComplete(
                Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> contentComplete) {
            this.contentComplete = contentComplete;
            return this;
        }

        public ClientHTTPHandler.Adapter messageComplete(
                Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> messageComplete) {
            this.messageComplete = messageComplete;
            return this;
        }

        public ClientHTTPHandler.Adapter badMessage(
                Action6<Integer, String, Request, Response, HTTPOutputStream, HTTPConnection> badMessage) {
            this.badMessage = badMessage;
            return this;
        }

        public ClientHTTPHandler.Adapter earlyEOF(
                Action4<Request, Response, HTTPOutputStream, HTTPConnection> earlyEOF) {
            this.earlyEOF = earlyEOF;
            return this;
        }

        public ClientHTTPHandler.Adapter continueToSendData(
                Action4<Request, Response, HTTPOutputStream, HTTPConnection> continueToSendData) {
            this.continueToSendData = continueToSendData;
            return this;
        }

        @Override
        public void continueToSendData(Request request, Response response, HTTPOutputStream output,
                                       HTTPConnection connection) {
            if (continueToSendData != null) {
                continueToSendData.call(request, response, output, connection);
            }
        }

    }
}
