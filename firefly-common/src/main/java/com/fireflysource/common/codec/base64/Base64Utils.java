package com.fireflysource.common.codec.base64;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class Base64Utils {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final Base64Delegate delegate = new CommonsCodecBase64Delegate();

    /**
     * Base64-encode the given byte array.
     *
     * @param src the original byte array (may be {@code null})
     * @return the encoded byte array (or {@code null} if the input was
     * {@code null})
     */
    public static byte[] encode(byte[] src) {
        return delegate.encode(src);
    }

    /**
     * Base64-decode the given byte array.
     *
     * @param src the encoded byte array (may be {@code null})
     * @return the original byte array (or {@code null} if the input was
     * {@code null})
     */
    public static byte[] decode(byte[] src) {
        return delegate.decode(src);
    }

    /**
     * Base64-encode the given byte array using the RFC 4868
     * "URL and Filename Safe Alphabet".
     *
     * @param src the original byte array (may be {@code null})
     * @return the encoded byte array (or {@code null} if the input was
     * {@code null})
     */
    public static byte[] encodeUrlSafe(byte[] src) {
        return delegate.encodeUrlSafe(src);
    }

    /**
     * Base64-decode the given byte array using the RFC 4868
     * "URL and Filename Safe Alphabet".
     *
     * @param src the encoded byte array (may be {@code null})
     * @return the original byte array (or {@code null} if the input was
     * {@code null})
     */
    public static byte[] decodeUrlSafe(byte[] src) {
        return delegate.decodeUrlSafe(src);
    }

    /**
     * Base64-encode the given byte array to a String.
     *
     * @param src the original byte array (may be {@code null})
     * @return the encoded byte array as a UTF-8 String (or {@code null} if the
     * input was {@code null})
     */
    public static String encodeToString(byte[] src) {
        if (src == null) {
            return null;
        }
        if (src.length == 0) {
            return "";
        }

        return new String(delegate.encode(src), DEFAULT_CHARSET);
    }

    /**
     * Base64-decode the given byte array from an UTF-8 String.
     *
     * @param src the encoded UTF-8 String (may be {@code null})
     * @return the original byte array (or {@code null} if the input was
     * {@code null})
     */
    public static byte[] decodeFromString(String src) {
        if (src == null) {
            return null;
        }
        if (src.length() == 0) {
            return new byte[0];
        }

        return delegate.decode(src.getBytes(DEFAULT_CHARSET));
    }

    /**
     * Base64-encode the given byte array to a String using the RFC 4868
     * "URL and Filename Safe Alphabet".
     *
     * @param src the original byte array (may be {@code null})
     * @return the encoded byte array as a UTF-8 String (or {@code null} if the
     * input was {@code null})
     */
    public static String encodeToUrlSafeString(byte[] src) {
        return new String(delegate.encodeUrlSafe(src), DEFAULT_CHARSET);
    }

    /**
     * Base64-decode the given byte array from an UTF-8 String using the RFC
     * 4868 "URL and Filename Safe Alphabet".
     *
     * @param src the encoded UTF-8 String (may be {@code null})
     * @return the original byte array (or {@code null} if the input was
     * {@code null})
     */
    public static byte[] decodeFromUrlSafeString(String src) {
        return delegate.decodeUrlSafe(src.getBytes(DEFAULT_CHARSET));
    }

    interface Base64Delegate {

        byte[] encode(byte[] src);

        byte[] decode(byte[] src);

        byte[] encodeUrlSafe(byte[] src);

        byte[] decodeUrlSafe(byte[] src);
    }

    static class CommonsCodecBase64Delegate implements Base64Delegate {

        private final Base64 base64 = new Base64();

        private final Base64 base64UrlSafe = new Base64(0, null, true);

        @Override
        public byte[] encode(byte[] src) {
            return this.base64.encode(src);
        }

        @Override
        public byte[] decode(byte[] src) {
            return this.base64.decode(src);
        }

        @Override
        public byte[] encodeUrlSafe(byte[] src) {
            return this.base64UrlSafe.encode(src);
        }

        @Override
        public byte[] decodeUrlSafe(byte[] src) {
            return this.base64UrlSafe.decode(src);
        }

    }
}
