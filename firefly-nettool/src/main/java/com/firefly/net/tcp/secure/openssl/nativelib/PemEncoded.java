package com.firefly.net.tcp.secure.openssl.nativelib;


import java.nio.ByteBuffer;

/**
 * A marker interface for PEM encoded values.
 */
interface PemEncoded {

    /**
     * Returns {@code true} if the PEM encoded value is considered
     * sensitive information such as a private key.
     */
    boolean isSensitive();

    ByteBuffer content();

    PemEncoded copy();

    PemEncoded duplicate();

    PemEncoded replace(ByteBuffer content);

}
