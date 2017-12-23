package com.firefly.net.tcp.codec.stream;

import com.firefly.net.tcp.codec.model.ClientRequest;
import com.firefly.net.tcp.codec.model.ClientResponse;
import com.firefly.net.tcp.codec.model.Request;
import com.firefly.utils.concurrent.Promise;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksClientConnection extends FfsocksConnection {

    <T> CompletableFuture<ClientResponse<T>> send(ClientRequest<T> request);

    void send(Request request, Promise<OutputStream> promise, Listener listener);

    interface Listener {

        void metaInfoComplete(FfsocksContext context);

        void content(FfsocksContext context, byte[] data);

        void messageComplete(FfsocksContext context);
        
    }
}
