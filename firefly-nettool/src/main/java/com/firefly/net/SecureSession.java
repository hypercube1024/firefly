package com.firefly.net;

import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public interface SecureSession extends Closeable, ApplicationProtocolSelector {

    boolean isOpen();

    ByteBuffer read(ByteBuffer receiveBuffer) throws IOException;

    int write(ByteBuffer[] outputBuffers, Callback callback) throws IOException;

    int write(ByteBuffer outputBuffer, Callback callback) throws IOException;

    long transferFileRegion(FileRegion file, Callback callback) throws IOException;

    boolean isHandshakeFinished();

    boolean isClientMode();
}
