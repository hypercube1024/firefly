package com.firefly.codec.http2.stream;

import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.function.Action1;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * @author Pengtao Qiu
 */
public interface HTTPTunnelConnection extends HTTPConnection {

    void write(ByteBuffer byteBuffer, Callback callback);

    void write(ByteBuffer[] buffers, Callback callback);

    void write(Collection<ByteBuffer> buffers, Callback callback);

    void write(FileRegion file, Callback callback);

    void content(Action1<ByteBuffer> content);

}
