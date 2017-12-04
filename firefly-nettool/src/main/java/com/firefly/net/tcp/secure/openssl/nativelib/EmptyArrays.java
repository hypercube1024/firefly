package com.firefly.net.tcp.secure.openssl.nativelib;

import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public final class EmptyArrays {

    public static final int[] EMPTY_INTS = {};
    public static final byte[] EMPTY_BYTES = {};
    public static final char[] EMPTY_CHARS = {};
    public static final Object[] EMPTY_OBJECTS = {};
    public static final Class<?>[] EMPTY_CLASSES = {};
    public static final String[] EMPTY_STRINGS = {};
    public static final StackTraceElement[] EMPTY_STACK_TRACE = {};
    public static final ByteBuffer[] EMPTY_BYTE_BUFFERS = {};
    public static final Certificate[] EMPTY_CERTIFICATES = {};
    public static final X509Certificate[] EMPTY_X509_CERTIFICATES = {};
    public static final javax.security.cert.X509Certificate[] EMPTY_JAVAX_X509_CERTIFICATES = {};

    private EmptyArrays() { }
}
