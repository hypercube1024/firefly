package com.firefly.net.tcp.secure;

import java.io.File;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class FileCertificateOpenSSLContextFactory extends AbstractOpenSSLSecureSessionFactory {
    private final String certificatePath;
    private final String privateKeyPath;

    public FileCertificateOpenSSLContextFactory(String certificatePath, String privateKeyPath) {
        this(DEFAULT_SUPPORTED_PROTOCOLS, certificatePath, privateKeyPath);
    }

    public FileCertificateOpenSSLContextFactory(List<String> supportedProtocols, String certificatePath, String privateKeyPath) {
        super(supportedProtocols);
        this.certificatePath = certificatePath;
        this.privateKeyPath = privateKeyPath;
    }

    @Override
    public File getCertificate() {
        return new File(certificatePath);
    }

    @Override
    public File getPrivateKey() {
        return new File(privateKeyPath);
    }
}
