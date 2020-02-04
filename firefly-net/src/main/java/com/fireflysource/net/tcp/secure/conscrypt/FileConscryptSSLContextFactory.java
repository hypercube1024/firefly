package com.fireflysource.net.tcp.secure.conscrypt;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author Pengtao Qiu
 */
public class FileConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {

    private SSLContext sslContext;

    public FileConscryptSSLContextFactory(String path, String keystorePassword, String keyPassword) {
        this(new File(path), keystorePassword, keyPassword, null, null, null);
    }

    public FileConscryptSSLContextFactory(File file, String keystorePassword, String keyPassword,
                                          String keyManagerFactoryType,
                                          String trustManagerFactoryType,
                                          String sslProtocol) {
        try (FileInputStream in = new FileInputStream(file)) {
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
