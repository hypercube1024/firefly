package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.concurrent.Callback;

/**
 * @author Pengtao Qiu
 */
abstract public class DataFrameHandler {

    public static void handleDataFrame(DataFrame dataFrame, Callback callback,
                                 MetaData.Request request, MetaData.Response response,
                                 HTTPOutputStream output, HTTPConnection connection,
                                 HTTPHandler httpHandler) {
        try {
            httpHandler.content(dataFrame.getData(), request, response, output, connection);
            if (dataFrame.isEndStream()) {
                httpHandler.contentComplete(request, response, output, connection);
                httpHandler.messageComplete(request, response, output, connection);
            }
            callback.succeeded();
        } catch (Throwable t) {
            callback.failed(t);
        }
    }
}
