package com.fireflysource.net.http.common.v2.stream;

import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame;

public interface FlowControl {

    void onStreamCreated(Stream stream);

    void onStreamDestroyed(Stream stream);

    void updateInitialStreamWindow(Http2Connection http2Connection, int initialStreamWindow, boolean local);

    void onWindowUpdate(Http2Connection http2Connection, Stream stream, WindowUpdateFrame frame);

    void onDataReceived(Http2Connection http2Connection, Stream stream, int length);

    void onDataConsumed(Http2Connection http2Connection, Stream stream, int length);

    void windowUpdate(Http2Connection http2Connection, Stream stream, WindowUpdateFrame frame);

    void onDataSending(Stream stream, int length);

    void onDataSent(Stream stream, int length);

}
