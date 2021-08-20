package com.fireflysource.net.tcp.secure.conscrypt;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class FileConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {

    private SSLContext sslContext;

    public FileConscryptSSLContextFactory(String path, String keystorePassword, String keyPassword, String keyStoreType) {
        this(FileConscryptSSLContextFactory.class.getClassLoader().getResourceAsStream(path), keystorePassword, keyPassword,
                keyStoreType, null, null, null);
    }

    public FileConscryptSSLContextFactory(
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
