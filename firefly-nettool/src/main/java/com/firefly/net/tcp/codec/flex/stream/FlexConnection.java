package com.firefly.net.tcp.codec.flex.stream;

import com.firefly.net.Connection;
import com.firefly.net.tcp.codec.flex.model.Request;

/**
 * @author Pengtao Qiu
 */
public interface FlexConnection extends Connection {

    Session getSession();

    void newRequest(Request request, Listener listener);

    void onRequest(Listener listener);

    FlexConfiguration getConfiguration();

    interface Listener {

        void newRequest(Context context);

        void newResponse(Context context);

        void content(Context context, byte[] receivedData);

        void contentComplete(Context context);

        void messageComplete(Context context);

        default void exception(Context context, Throwable throwable) {

        }

        default void close(Context context) {
            
        }
    }

}
