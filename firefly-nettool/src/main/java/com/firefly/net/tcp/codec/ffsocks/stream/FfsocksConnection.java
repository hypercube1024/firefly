package com.firefly.net.tcp.codec.ffsocks.stream;

import com.firefly.net.Connection;
import com.firefly.net.tcp.codec.ffsocks.model.Request;

/**
 * @author Pengtao Qiu
 */
public interface FfsocksConnection extends Connection {

    Session getSession();

    void newRequest(Request request, Listener listener);

    void onRequest(Listener listener);

    FfsocksConfiguration getConfiguration();

    interface Listener {

        void newRequest(Context context);

        void newResponse(Context context);

        void content(Context context, byte[] receivedData);

        void contentComplete(Context context);

        void messageComplete(Context context);
    }

}
