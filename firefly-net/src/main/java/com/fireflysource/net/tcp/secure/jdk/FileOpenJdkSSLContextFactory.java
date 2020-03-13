package com.fireflysource.net.tcp.secure.jdk;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class FileOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public FileOpenJdkSSLContextFactory(String path, String keystorePassword, String keyPassword) {
        this(FileOpenJdkSSLContextFactory.class.getClassLoader().getResourceAsStream(path), keystorePassword, keyPassword,
                null, null, null);
    }

    public FileOpenJdkSSLContextFactory(InputStream inputStream, String keystorePassword, String keyPassword,
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
