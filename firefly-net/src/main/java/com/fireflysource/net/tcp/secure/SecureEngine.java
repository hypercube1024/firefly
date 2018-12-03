package com.fireflysource.net.tcp.secure;

import com.fireflysource.net.tcp.Result;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author Pengtao Qiu
 */
public interface SecureEngine extends Cloneable, ApplicationProtocolSelector {

    void beginHandshake(Consumer<Result<SecureEngine>> result);

    ByteBuffer decode(ByteBuffer byteBuffer);

    ByteBuffer encode(ByteBuffer byteBuffer);

}
