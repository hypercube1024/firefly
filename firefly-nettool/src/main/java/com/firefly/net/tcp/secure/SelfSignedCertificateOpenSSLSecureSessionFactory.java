package com.firefly.net.tcp.secure;

import com.firefly.net.tcp.secure.openssl.SelfSignedCertificate;
import com.firefly.utils.exception.CommonRuntimeException;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateOpenSSLSecureSessionFactory extends AbstractOpenSSLSecureSessionFactory {

    private SelfSignedCertificate selfSignedCertificate;

    public SelfSignedCertificateOpenSSLSecureSessionFactory() {
        this(DEFAULT_SUPPORTED_PROTOCOLS);
    }

    public SelfSignedCertificateOpenSSLSecureSessionFactory(List<String> supportedProtocols) {
        super(supportedProtocols);
        try {
            selfSignedCertificate = new SelfSignedCertificate("www.fireflysource.com");
        } catch (CertificateException e) {
            log.error("create certificate exception", e);
            throw new CommonRuntimeException(e);
        }
    }

    public SelfSignedCertificate getSelfSignedCertificate() {
        return selfSignedCertificate;
    }

    public void setSelfSignedCertificate(SelfSignedCertificate selfSignedCertificate) {
        this.selfSignedCertificate = selfSignedCertificate;
    }

    @Override
    public File getCertificate() {
        return selfSignedCertificate.certificate();
    }

    @Override
    public File getPrivateKey() {
        return selfSignedCertificate.privateKey();
    }
}
