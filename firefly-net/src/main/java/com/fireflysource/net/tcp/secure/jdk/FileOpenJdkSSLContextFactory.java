package com.fireflysource.net.tcp.secure.jdk;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class FileOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public FileOpenJdkSSLContextFactory(String path, String keystorePassword, String keyPassword, String keyStoreType) {
        this(FileOpenJdkSSLContextFactory.class.getClassLoader().getResourceAsStream(path),
                keystorePassword, keyPassword,
                keyStoreType, null, null, null);
    }

    public FileOpenJdkSSLContextFactory(
            InputStream inputStream, String keystorePassword, String keyPassword,
            String keyStoreType,
            String keyManagerFactoryType,
            String trustManagerFactoryType,
            String sslProtocol) {
        try (InputStream in = inputStream) {
            sslContext = getSSLContext(in, keystorePassword, keyPassword,
                    keyStoreType, keyManagerFactoryType, trustManagerFactoryType, sslProtocol);
        } catch (Exception e) {
            LOG.error(e, () -> "get SSL context exception");
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
