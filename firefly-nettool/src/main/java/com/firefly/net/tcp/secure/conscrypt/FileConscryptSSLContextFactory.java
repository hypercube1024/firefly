package com.firefly.net.tcp.secure.conscrypt;

import com.firefly.net.tcp.secure.jdk.AbstractJdkSSLContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author Pengtao Qiu
 */
public class FileConscryptSSLContextFactory extends AbstractJdkSSLContextFactory {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

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
            log.error("get SSL context exception", e);
        }
        return ret;
    }
}
