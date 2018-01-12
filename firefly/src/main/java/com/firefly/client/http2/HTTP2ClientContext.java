package com.firefly.client.http2;

import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.utils.concurrent.Promise;

public class HTTP2ClientContext {
    private Promise<HTTPClientConnection> promise;
    private ClientHTTP2SessionListener listener;

    public Promise<HTTPClientConnection> getPromise() {
        return promise;
    }

    public void setPromise(Promise<HTTPClientConnection> promise) {
        this.promise = promise;
    }

    public ClientHTTP2SessionListener getListener() {
        return listener;
    }

    public void setListener(ClientHTTP2SessionListener listener) {
        this.listener = listener;
    }
}
