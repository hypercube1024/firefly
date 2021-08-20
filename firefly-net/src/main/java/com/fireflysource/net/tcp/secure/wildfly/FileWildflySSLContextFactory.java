package com.fireflysource.net.tcp.secure.wildfly;

import com.fireflysource.net.tcp.secure.jdk.FileOpenJdkSSLContextFactory;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

public class FileWildflySSLContextFactory extends AbstractWildflySecureEngineFactory {
    private SSLContext sslContext;

    public FileWildflySSLContextFactory(String path, String keystorePassword, String keyPassword, String keyStoreType) {
        this(FileOpenJdkSSLContextFactory.class.getClassLoader().getResourceAsStream(path),
                keystorePassword, keyPassword,
                keyStoreType, null, null, null);
    }

    public FileWildflySSLContextFactory(
            InputStream inputStream, String keystorePassword, String keyPassword,
            String keyStoreType,
            String keyManagerFactoryType,
            String trustManagerFactoryType,
            String sslProtocol) {
        try (InputStream in = inputStream) {
            sslContext = getSSLContext(in, keystorePassword, keyPassword,
                    keyStoreType, keyManagerFactoryType, trustManagerFactoryType, sslProtocol);
        } catch (Exception e) {
            LOG.error("get SSL context exception", e);
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
