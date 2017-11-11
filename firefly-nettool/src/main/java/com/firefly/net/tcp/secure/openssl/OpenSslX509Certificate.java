package com.firefly.net.tcp.secure.openssl;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.util.Date;
import java.util.Set;

final class OpenSslX509Certificate extends X509Certificate {

    private final byte[] bytes;
    private X509Certificate wrapped;

    public OpenSslX509Certificate(byte[] bytes) {
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
    public byte[] getTBSCertificate() throws CertificateEncodingException {
        return unwrap().getTBSCertificate();
    }

    @Override
    public byte[] getSignature() {
        return unwrap().getSignature();
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
    public boolean[] getIssuerUniqueID() {
        return unwrap().getIssuerUniqueID();
    }

    @Override
    public boolean[] getSubjectUniqueID() {
        return unwrap().getSubjectUniqueID();
    }

    @Override
    public boolean[] getKeyUsage() {
        return unwrap().getKeyUsage();
    }

    @Override
    public int getBasicConstraints() {
        return unwrap().getBasicConstraints();
    }

    @Override
    public byte[] getEncoded() {
        return bytes.clone();
    }

    @Override
    public void verify(PublicKey key)
            throws CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException, SignatureException {
        unwrap().verify(key);
    }

    @Override
    public void verify(PublicKey key, String sigProvider)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException {
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

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        return unwrap().hasUnsupportedCriticalExtension();
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        return unwrap().getCriticalExtensionOIDs();
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        return unwrap().getNonCriticalExtensionOIDs();
    }

    @Override
    public byte[] getExtensionValue(String oid) {
        return unwrap().getExtensionValue(oid);
    }

    private X509Certificate unwrap() {
        X509Certificate wrapped = this.wrapped;
        if (wrapped == null) {
            try {
                wrapped = this.wrapped = (X509Certificate) SslContext.X509_CERT_FACTORY.generateCertificate(
                        new ByteArrayInputStream(bytes));
            } catch (CertificateException e) {
                throw new IllegalStateException(e);
            }
        }
        return wrapped;
    }
}
