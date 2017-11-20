package com.firefly.net.tcp.secure.openssl.nativelib;

import com.firefly.utils.codec.Base64;
import com.firefly.utils.io.BufferUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * This is a special purpose implementation of a {@link X509Certificate} which allows
 * the user to pass PEM/PKCS#8 encoded data straight into {@link OpenSslContext} without
 * having to parse and re-encode bytes in Java land.
 * <p>
 * All methods other than what's implemented in {@link PemEncoded}'s throw
 * {@link UnsupportedOperationException}s.
 *
 * @see PemEncoded
 * @see OpenSslContext
 * @see #valueOf(byte[])
 */
public final class PemX509Certificate extends X509Certificate implements PemEncoded {

    private static final byte[] BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] END_CERT = "-----END CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII);

    /**
     * Creates a {@link PemEncoded} value from the {@link X509Certificate}s.
     */
    static PemEncoded toPEM(X509Certificate... chain) throws CertificateEncodingException {

        if (chain == null || chain.length == 0) {
            throw new IllegalArgumentException("X.509 certificate chain can't be null or empty");
        }

        // We can take a shortcut if there is only one certificate and
        // it already happens to be a PemEncoded instance. This is the
        // ideal case and reason why all this exists. It allows the user
        // to pass pre-encoded bytes straight into OpenSSL without having
        // to do any of the extra work.
        if (chain.length == 1) {
            X509Certificate first = chain[0];
            if (first instanceof PemEncoded) {
                return ((PemEncoded) first);
            }
        }

        ByteBuffer pem = null;
        for (X509Certificate cert : chain) {

            if (cert == null) {
                throw new IllegalArgumentException("Null element in chain: " + Arrays.toString(chain));
            }

            if (cert instanceof PemEncoded) {
                pem = append((PemEncoded) cert, chain.length, pem);
            } else {
                pem = append(cert, chain.length, pem);
            }
        }
        Optional.ofNullable(pem).ifPresent(ByteBuffer::flip);
        return new PemValue(pem, false);
    }

    /**
     * Appends the {@link PemEncoded} value to the {@link ByteBuffer} (last arg) and returns it.
     * If the {@link ByteBuffer} didn't exist yet it'll create it.
     */
    private static ByteBuffer append(PemEncoded encoded, int count, ByteBuffer pem) {
        ByteBuffer content = encoded.content();
        if (pem == null) {
            // see the other append() method
            pem = ByteBuffer.allocateDirect(content.remaining() * count * 2);
        }

        pem.put(content.duplicate());
        return pem;
    }

    /**
     * Appends the {@link X509Certificate} value to the {@link ByteBuffer} (last arg) and returns it.
     */
    private static ByteBuffer append(X509Certificate cert, int count, ByteBuffer pem) throws CertificateEncodingException {
        byte[] base64 = Base64.encodeBase64(cert.getEncoded(), true);
        if (pem == null) {
            // We try to approximate the buffer's initial size. The sizes of
            // certificates can vary a lot so it'll be off a bit depending
            // on the number of elements in the array (count argument).
            int length = (BEGIN_CERT.length + base64.length + END_CERT.length) * count * 2;
            pem = ByteBuffer.allocateDirect(length);
        }
        pem.put(BEGIN_CERT).put(base64, 0, base64.length).put(END_CERT);
        return pem;
    }

    /**
     * Creates a {@link PemX509Certificate} from raw {@code byte[]}.
     * <p>
     * ATTENTION: It's assumed that the given argument is a PEM/PKCS#8 encoded value.
     * No input validation is performed to validate it.
     */
    public static PemX509Certificate valueOf(byte[] key) {
        ByteBuffer tmp = ByteBuffer.allocateDirect(key.length);
        tmp.put(key).flip();
        return valueOf(tmp);
    }

    /**
     * Creates a {@link PemX509Certificate} from raw {@code ByteBuf}.
     * <p>
     * ATTENTION: It's assumed that the given argument is a PEM/PKCS#8 encoded value.
     * No input validation is performed to validate it.
     */
    public static PemX509Certificate valueOf(ByteBuffer key) {
        return new PemX509Certificate(key);
    }

    private final ByteBuffer content;

    private PemX509Certificate(ByteBuffer content) {
        this.content = ObjectUtil.checkNotNull(content, "content");
    }

    @Override
    public boolean isSensitive() {
        // There is no sensitive information in a X509 Certificate
        return false;
    }

    @Override
    public ByteBuffer content() {
        return content;
    }

    @Override
    public PemX509Certificate copy() {
        ByteBuffer tmp = ByteBuffer.allocateDirect(content.remaining());
        tmp.put(content.duplicate()).flip();
        return replace(tmp);
    }

    @Override
    public PemX509Certificate duplicate() {
        return replace(content.duplicate());
    }

    @Override
    public PemX509Certificate replace(ByteBuffer content) {
        return new PemX509Certificate(content);
    }

    @Override
    public byte[] getEncoded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getExtensionValue(String oid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkValidity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkValidity(Date date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigInteger getSerialNumber() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getIssuerDN() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getSubjectDN() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getNotBefore() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getNotAfter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getTBSCertificate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getSignature() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSigAlgName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSigAlgOID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getSigAlgParams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getIssuerUniqueID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getSubjectUniqueID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getKeyUsage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBasicConstraints() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void verify(PublicKey key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void verify(PublicKey key, String sigProvider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PublicKey getPublicKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PemX509Certificate)) {
            return false;
        }

        PemX509Certificate other = (PemX509Certificate) o;
        return content.equals(other.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public String toString() {
        return BufferUtils.toString(content, StandardCharsets.UTF_8);
    }
}
