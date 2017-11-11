package com.firefly.net.tcp.secure;

import com.firefly.net.tcp.secure.openssl.*;
import com.firefly.utils.exception.CommonRuntimeException;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateOpenSSLContextFactory extends AbstractOpenSSLSecureSessionFactory {

    private SelfSignedCertificate selfSignedCertificate;
    private List<String> supportedProtocols;

    public SelfSignedCertificateOpenSSLContextFactory() {
        this(Arrays.asList("h2", "http/1.1"));
    }

    public SelfSignedCertificateOpenSSLContextFactory(List<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
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

    public List<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    public void setSupportedProtocols(List<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public SslContext createSSLContext(boolean clientMode) {
        SslContextBuilder sslContextBuilder = clientMode
                ? SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                : SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey());

        try {
            return sslContextBuilder.ciphers(SecurityUtils.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                            ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            supportedProtocols)).build();
        } catch (SSLException e) {
            log.error("create ssl context exception", e);
            throw new CommonRuntimeException(e);
        }
    }
}
