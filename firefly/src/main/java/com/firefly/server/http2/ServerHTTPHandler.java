package com.firefly.server.http2;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPHandler;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.function.*;

import java.nio.ByteBuffer;

public interface ServerHTTPHandler extends HTTPHandler {

    void acceptConnection(HTTPConnection connection);

    boolean accept100Continue(MetaData.Request request, MetaData.Response response,
                              HTTPOutputStream output,
                              HTTPConnection connection);

    boolean acceptHTTPTunnelConnection(MetaData.Request request, MetaData.Response response,
                                       HTTPOutputStream output,
                                       HTTPConnection connection);

    class Adapter extends HTTPHandler.Adapter implements ServerHTTPHandler {

        protected Action1<HTTPConnection> acceptConnection;
        protected Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> accept100Continue;
        protected Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> acceptHTTPTunnelConnection;

        public ServerHTTPHandler.Adapter messageComplete(
                Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> messageComplete) {
            this.messageComplete = messageComplete;
            return this;
        }

        public ServerHTTPHandler.Adapter headerComplete(
                Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> headerComplete) {
            this.headerComplete = headerComplete;
            return this;
        }

        public ServerHTTPHandler.Adapter content(
                Func5<ByteBuffer, Request, Response, HTTPOutputStream, HTTPConnection, Boolean> content) {
            this.content = content;
            return this;
        }

        public ServerHTTPHandler.Adapter badMessage(
                Action6<Integer, String, Request, Response, HTTPOutputStream, HTTPConnection> badMessage) {
            this.badMessage = badMessage;
            return this;
        }

        public ServerHTTPHandler.Adapter earlyEOF(
                Action4<Request, Response, HTTPOutputStream, HTTPConnection> earlyEOF) {
            this.earlyEOF = earlyEOF;
            return this;
        }

        public ServerHTTPHandler.Adapter acceptConnection(Action1<HTTPConnection> acceptConnection) {
            this.acceptConnection = acceptConnection;
            return this;
        }

        public ServerHTTPHandler.Adapter accept100Continue(
                Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> accept100Continue) {
            this.accept100Continue = accept100Continue;
            return this;
        }

        @Override
        public void acceptConnection(HTTPConnection connection) {
            if (acceptConnection != null) {
                acceptConnection.call(connection);
            }
        }

        @Override
        public boolean accept100Continue(Request request, Response response, HTTPOutputStream output,
                                         HTTPConnection connection) {
            if (accept100Continue != null) {
                return accept100Continue.call(request, response, output, connection);
            } else {
                return false;
            }
        }

        @Override
        public boolean acceptHTTPTunnelConnection(MetaData.Request request, MetaData.Response response,
                                           HTTPOutputStream output,
                                           HTTPConnection connection) {
            if (acceptHTTPTunnelConnection != null) {
                return acceptHTTPTunnelConnection.call(request, response, output, connection);
            } else {
                return false;
            }
        }

    }
}
