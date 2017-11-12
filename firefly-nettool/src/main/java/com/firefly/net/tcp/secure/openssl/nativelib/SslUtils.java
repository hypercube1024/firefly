package com.firefly.net.tcp.secure.openssl.nativelib;

import javax.net.ssl.SSLHandshakeException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Constants for SSL packets.
 */
final class SslUtils {

    // Protocols
    static final String PROTOCOL_SSL_V2_HELLO = "SSLv2Hello";
    static final String PROTOCOL_SSL_V2 = "SSLv2";
    static final String PROTOCOL_SSL_V3 = "SSLv3";
    static final String PROTOCOL_TLS_V1 = "TLSv1";
    static final String PROTOCOL_TLS_V1_1 = "TLSv1.1";
    static final String PROTOCOL_TLS_V1_2 = "TLSv1.2";

    /**
     * change cipher spec
     */
    static final int SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC = 20;

    /**
     * alert
     */
    static final int SSL_CONTENT_TYPE_ALERT = 21;

    /**
     * handshake
     */
    static final int SSL_CONTENT_TYPE_HANDSHAKE = 22;

    /**
     * application data
     */
    static final int SSL_CONTENT_TYPE_APPLICATION_DATA = 23;

    /**
     * HeartBeat Extension
     */
    static final int SSL_CONTENT_TYPE_EXTENSION_HEARTBEAT = 24;

    /**
     * the length of the ssl record header (in bytes)
     */
    static final int SSL_RECORD_HEADER_LENGTH = 5;

    /**
     * Not enough data in buffer to parse the record length
     */
    static final int NOT_ENOUGH_DATA = -1;

    /**
     * data is not encrypted
     */
    static final int NOT_ENCRYPTED = -2;

    static final String[] DEFAULT_CIPHER_SUITES = {
            // GCM (Galois/Counter Mode) requires JDK 8.
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            // AES256 requires JCE unlimited strength jurisdiction policy files.
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            // GCM (Galois/Counter Mode) requires JDK 8.
            "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            // AES256 requires JCE unlimited strength jurisdiction policy files.
            "TLS_RSA_WITH_AES_256_CBC_SHA"
    };

    /**
     * Add elements from {@code names} into {@code enabled} if they are in {@code supported}.
     */
    static void addIfSupported(Set<String> supported, List<String> enabled, String... names) {
        for (String n : names) {
            if (supported.contains(n)) {
                enabled.add(n);
            }
        }
    }

    static void useFallbackCiphersIfDefaultIsEmpty(List<String> defaultCiphers, Iterable<String> fallbackCiphers) {
        if (defaultCiphers.isEmpty()) {
            for (String cipher : fallbackCiphers) {
                if (cipher.startsWith("SSL_") || cipher.contains("_RC4_")) {
                    continue;
                }
                defaultCiphers.add(cipher);
            }
        }
    }

    static void useFallbackCiphersIfDefaultIsEmpty(List<String> defaultCiphers, String... fallbackCiphers) {
        useFallbackCiphersIfDefaultIsEmpty(defaultCiphers, asList(fallbackCiphers));
    }

    /**
     * Converts the given exception to a {@link SSLHandshakeException}, if it isn't already.
     */
    static SSLHandshakeException toSSLHandshakeException(Throwable e) {
        if (e instanceof SSLHandshakeException) {
            return (SSLHandshakeException) e;
        }

        return (SSLHandshakeException) new SSLHandshakeException(e.getMessage()).initCause(e);
    }

    private static short unsignedByte(byte b) {
        return (short) (b & 0xFF);
    }

    // Reads a big-endian unsigned short integer from the buffer
    private static int unsignedShortBE(ByteBuffer buffer, int offset) {
        return shortBE(buffer, offset) & 0xFFFF;
    }

    // Reads a big-endian short integer from the buffer
    private static short shortBE(ByteBuffer buffer, int offset) {
        return buffer.order() == ByteOrder.BIG_ENDIAN ?
                buffer.getShort(offset) : Short.reverseBytes(buffer.getShort(offset));
    }

    static int getEncryptedPacketLength(ByteBuffer[] buffers, int offset) {
        ByteBuffer buffer = buffers[offset];

        // Check if everything we need is in one ByteBuffer. If so we can make use of the fast-path.
        if (buffer.remaining() >= SSL_RECORD_HEADER_LENGTH) {
            return getEncryptedPacketLength(buffer);
        }

        // We need to copy 5 bytes into a temporary buffer so we can parse out the packet length easily.
        ByteBuffer tmp = ByteBuffer.allocate(5);

        do {
            buffer = buffers[offset++].duplicate();
            if (buffer.remaining() > tmp.remaining()) {
                buffer.limit(buffer.position() + tmp.remaining());
            }
            tmp.put(buffer);
        } while (tmp.hasRemaining());

        // Done, flip the buffer so we can read from it.
        tmp.flip();
        return getEncryptedPacketLength(tmp);
    }

    private static int getEncryptedPacketLength(ByteBuffer buffer) {
        int packetLength = 0;
        int pos = buffer.position();
        // SSLv3 or TLS - Check ContentType
        boolean tls;
        switch (unsignedByte(buffer.get(pos))) {
            case SSL_CONTENT_TYPE_CHANGE_CIPHER_SPEC:
            case SSL_CONTENT_TYPE_ALERT:
            case SSL_CONTENT_TYPE_HANDSHAKE:
            case SSL_CONTENT_TYPE_APPLICATION_DATA:
            case SSL_CONTENT_TYPE_EXTENSION_HEARTBEAT:
                tls = true;
                break;
            default:
                // SSLv2 or bad data
                tls = false;
        }

        if (tls) {
            // SSLv3 or TLS - Check ProtocolVersion
            int majorVersion = unsignedByte(buffer.get(pos + 1));
            if (majorVersion == 3) {
                // SSLv3 or TLS
                packetLength = unsignedShortBE(buffer, pos + 3) + SSL_RECORD_HEADER_LENGTH;
                if (packetLength <= SSL_RECORD_HEADER_LENGTH) {
                    // Neither SSLv3 or TLSv1 (i.e. SSLv2 or bad data)
                    tls = false;
                }
            } else {
                // Neither SSLv3 or TLSv1 (i.e. SSLv2 or bad data)
                tls = false;
            }
        }

        if (!tls) {
            // SSLv2 or bad data - Check the version
            int headerLength = (unsignedByte(buffer.get(pos)) & 0x80) != 0 ? 2 : 3;
            int majorVersion = unsignedByte(buffer.get(pos + headerLength + 1));
            if (majorVersion == 2 || majorVersion == 3) {
                // SSLv2
                packetLength = headerLength == 2 ?
                        (shortBE(buffer, pos) & 0x7FFF) + 2 : (shortBE(buffer, pos) & 0x3FFF) + 3;
                if (packetLength <= headerLength) {
                    return NOT_ENOUGH_DATA;
                }
            } else {
                return NOT_ENCRYPTED;
            }
        }
        return packetLength;
    }


    private SslUtils() {
    }
}
