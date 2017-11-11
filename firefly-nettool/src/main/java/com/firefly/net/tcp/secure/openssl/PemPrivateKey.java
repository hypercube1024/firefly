package com.firefly.net.tcp.secure.openssl;

import com.firefly.utils.codec.Base64;

import javax.security.auth.Destroyable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * This is a special purpose implementation of a {@link PrivateKey} which allows the
 * user to pass PEM/PKCS#8 encoded key material straight into {@link OpenSslContext}
 * without having to parse and re-encode bytes in Java land.
 * <p>
 * All methods other than what's implemented in {@link PemEncoded} and {@link Destroyable}
 * throw {@link UnsupportedOperationException}s.
 *
 * @see PemEncoded
 * @see OpenSslContext
 * @see #valueOf(byte[])
 */
public final class PemPrivateKey implements PrivateKey, PemEncoded {
    private static final long serialVersionUID = 7978017465645018936L;

    private static final byte[] BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] END_PRIVATE_KEY = "-----END PRIVATE KEY-----\n".getBytes(StandardCharsets.US_ASCII);

    private static final String PKCS8_FORMAT = "PKCS#8";

    /**
     * Creates a {@link PemEncoded} value from the {@link PrivateKey}.
     */
    static PemEncoded toPEM(PrivateKey key) {
        // We can take a shortcut if the private key happens to be already
        // PEM/PKCS#8 encoded. This is the ideal case and reason why all
        // this exists. It allows the user to pass pre-encoded bytes straight
        // into OpenSSL without having to do any of the extra work.
        if (key instanceof PemEncoded) {
            return (PemEncoded) key;
        }

        byte[] base64 = Base64.encodeBase64(key.getEncoded(), true);
        int size = BEGIN_PRIVATE_KEY.length + base64.length + END_PRIVATE_KEY.length;

        final ByteBuffer pem = ByteBuffer.allocateDirect(size);
        pem.put(BEGIN_PRIVATE_KEY).put(base64).put(END_PRIVATE_KEY).flip();
        return new PemValue(pem, true);
    }

    /**
     * Creates a {@link PemPrivateKey} from raw {@code byte[]}.
     * <p>
     * ATTENTION: It's assumed that the given argument is a PEM/PKCS#8 encoded value.
     * No input validation is performed to validate it.
     */
    public static PemPrivateKey valueOf(byte[] key) {
        ByteBuffer tmp = ByteBuffer.allocateDirect(key.length);
        tmp.put(key).flip();
        return valueOf(tmp);
    }

    /**
     * Creates a {@link PemPrivateKey} from raw {@code ByteBuf}.
     * <p>
     * ATTENTION: It's assumed that the given argument is a PEM/PKCS#8 encoded value.
     * No input validation is performed to validate it.
     */
    public static PemPrivateKey valueOf(ByteBuffer key) {
        return new PemPrivateKey(key);
    }

    private final ByteBuffer content;

    private PemPrivateKey(ByteBuffer content) {
        this.content = ObjectUtil.checkNotNull(content, "content");
    }

    @Override
    public boolean isSensitive() {
        return true;
    }

    @Override
    public ByteBuffer content() {
        return content;
    }

    @Override
    public PemPrivateKey copy() {
        ByteBuffer tmp = ByteBuffer.allocateDirect(content.remaining());
        tmp.put(content.duplicate()).flip();
        return replace(tmp);
    }

    @Override
    public PemPrivateKey duplicate() {
        return replace(content.duplicate());
    }

    @Override
    public PemPrivateKey replace(ByteBuffer content) {
        return new PemPrivateKey(content);
    }

    @Override
    public byte[] getEncoded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAlgorithm() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFormat() {
        return PKCS8_FORMAT;
    }

}
