package com.fireflysource.net.tcp.secure.conscrypt;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author Pengtao Qiu
 */
public class FileConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {

    private File file;
    private String keystorePassword;
    private String keyPassword;
    private String keyManagerFactoryType;
    private String trustManagerFactoryType;
    private String sslProtocol;

    public FileConscryptSSLContextFactory(String path, String keystorePassword, String keyPassword) {
        this(new File(path), keystorePassword, keyPassword, null, null, null);
    }

    public FileConscryptSSLContextFactory(File file, String keystorePassword, String keyPassword,
                                          String keyManagerFactoryType,
                                          String trustManagerFactoryType,
                                          String sslProtocol) {
        this.file = file;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
        this.keyManagerFactoryType = keyManagerFactoryType;
        this.trustManagerFactoryType = trustManagerFactoryType;
        this.sslProtocol = sslProtocol;
    }

    @Override
    public SSLContext getSSLContext() {
        SSLContext ret = null;
        try (FileInputStream in = new FileInputStream(file)) {
            ret = getSSLContext(in, keystorePassword, keyPassword, keyManagerFactoryType, trustManagerFactoryType, sslProtocol);
        } catch (Exception e) {
            log.error(e, () -> "get SSL context exception");
        }
        return ret;
    }
}
