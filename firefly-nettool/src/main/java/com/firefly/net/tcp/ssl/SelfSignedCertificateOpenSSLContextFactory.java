package com.firefly.net.tcp.ssl;

import com.firefly.utils.exception.CommonRuntimeException;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateOpenSSLContextFactory extends AbstractOpenSSLContextFactory {

    private SelfSignedCertificate selfSignedCertificate;

    public SelfSignedCertificateOpenSSLContextFactory() {
        try {
            selfSignedCertificate = new SelfSignedCertificate("www.fireflysource.com");
        } catch (CertificateException e) {
            log.error("create certificate exception", e);
            throw new CommonRuntimeException(e);
        }
    }

    public SelfSignedCertificateOpenSSLContextFactory(SelfSignedCertificate selfSignedCertificate) {
        this.selfSignedCertificate = selfSignedCertificate;
    }

    public SelfSignedCertificateOpenSSLContextFactory(ByteBufAllocator byteBufAllocator, SelfSignedCertificate selfSignedCertificate) {
        super(byteBufAllocator);
        this.selfSignedCertificate = selfSignedCertificate;
    }

    @Override
    public void createSSLContext(boolean clientMode) {
        if (clientMode) {
            try {
                sslContext = SslContextBuilder.forClient()
                                              .build();
            } catch (SSLException e) {
                log.error("create client ssl context exception", e);
                throw new CommonRuntimeException(e);
            }
        } else {
            try {
                sslContext = SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                                              .applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                                      ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                                      ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                                      "h2", "h2-17", "h2-16", "h2-15", "h2-14", "http/1.1"))
                                              .build();
            } catch (SSLException e) {
                log.error("create server ssl context exception", e);
                throw new CommonRuntimeException(e);
            }
        }
    }
}
