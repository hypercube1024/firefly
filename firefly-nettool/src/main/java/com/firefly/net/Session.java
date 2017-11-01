package com.firefly.net;

import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

public interface Session {

    DisconnectionOutputEntry DISCONNECTION_FLAG = new DisconnectionOutputEntry(null, null);

    enum State {
        CLOSE, OPEN, INPUT_SHUTDOWN, OUTPUT_SHUTDOWN
    }

    void attachObject(Object attachment);

    Object getAttachment();

    void fireReceiveMessage(Object message);

    void encode(Object message);

    void write(OutputEntry<?> entry);

    void write(ByteBuffer byteBuffer, Callback callback);

    void write(ByteBuffer[] buffers, Callback callback);

    void write(Collection<ByteBuffer> buffers, Callback callback);

    void write(FileRegion file, Callback callback);

    int getSessionId();

    long getOpenTime();

    long getCloseTime();

    long getDuration();

    long getLastReadTime();

    long getLastWrittenTime();

    long getLastActiveTime();

    long getReadBytes();

    long getWrittenBytes();

    void close();

    void closeNow();

    void shutdownOutput();

    void shutdownInput();

    State getState();

    boolean isOpen();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

    long getIdleTimeout();
}
