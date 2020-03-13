package com.fireflysource.net.tcp.secure.conscrypt;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class FileConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {

    private SSLContext sslContext;

    public FileConscryptSSLContextFactory(String path, String keystorePassword, String keyPassword) {
        this(FileConscryptSSLContextFactory.class.getClassLoader().getResourceAsStream(path), keystorePassword, keyPassword,
                null, null, null);
    }

    public FileConscryptSSLContextFactory(InputStream inputStream, String keystorePassword, String keyPassword,
                                          String keyManagerFactoryType,
                                          String trustManagerFactoryType,
                                          String sslProtocol) {
        try (InputStream in = inputStream) {
            sslContext = getSSLContext(in, keystorePassword, keyPassword, keyManagerFactoryType, trustManagerFactoryType, sslProtocol);
        } catch (Exception e) {
            LOG.error(e, () -> "get SSL context exception");
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
