package com.firefly.net.tcp.secure.openssl.nativelib;

import javax.security.cert.CertificateException;
import javax.security.cert.CertificateExpiredException;
import javax.security.cert.CertificateNotYetValidException;
import javax.security.cert.X509Certificate;
import java.math.BigInteger;
import java.security.*;
import java.util.Date;

final class OpenSslJavaxX509Certificate extends X509Certificate {
    private final byte[] bytes;
    private X509Certificate wrapped;

    public OpenSslJavaxX509Certificate(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        unwrap().checkValidity();
    }

    @Override
    public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
        unwrap().checkValidity(date);
    }

    @Override
    public int getVersion() {
        return unwrap().getVersion();
    }

    @Override
    public BigInteger getSerialNumber() {
        return unwrap().getSerialNumber();
    }

    @Override
    public Principal getIssuerDN() {
        return unwrap().getIssuerDN();
    }

    @Override
    public Principal getSubjectDN() {
        return unwrap().getSubjectDN();
    }

    @Override
    public Date getNotBefore() {
        return unwrap().getNotBefore();
    }

    @Override
    public Date getNotAfter() {
        return unwrap().getNotAfter();
    }

    @Override
    public String getSigAlgName() {
        return unwrap().getSigAlgName();
    }

    @Override
    public String getSigAlgOID() {
        return unwrap().getSigAlgOID();
    }

    @Override
    public byte[] getSigAlgParams() {
        return unwrap().getSigAlgParams();
    }

    @Override
    public byte[] getEncoded() {
        return bytes.clone();
    }

    @Override
    public void verify(PublicKey key)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
                   SignatureException {
        unwrap().verify(key);
    }

    @Override
    public void verify(PublicKey key, String sigProvider)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
                   SignatureException {
        unwrap().verify(key, sigProvider);
    }

    @Override
    public String toString() {
        return unwrap().toString();
    }

    @Override
    public PublicKey getPublicKey() {
        return unwrap().getPublicKey();
    }

    private X509Certificate unwrap() {
        X509Certificate wrapped = this.wrapped;
        if (wrapped == null) {
            try {
                wrapped = this.wrapped = X509Certificate.getInstance(bytes);
            } catch (CertificateException e) {
                throw new IllegalStateException(e);
            }
        }
        return wrapped;
    }
}
