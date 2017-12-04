package com.firefly.net.tcp;

import com.firefly.net.Config;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;

public class TcpConfiguration extends Config {

    // SSL/TLS settings
    private boolean isSecureConnectionEnabled;
    private SecureSessionFactory secureSessionFactory = new DefaultOpenSSLSecureSessionFactory();

    /**
     * If the secure connection is enabled, it will create secure connection using SSL/TLS protocol.
     *
     * @return isSecureConnectionEnabled. The default value is false.
     */
    public boolean isSecureConnectionEnabled() {
        return isSecureConnectionEnabled;
    }

    /**
     * If the secure connection is enabled, it will create secure connection using SSL/TLS protocol.
     *
     * @param isSecureConnectionEnabled The default value is false.
     */
    public void setSecureConnectionEnabled(boolean isSecureConnectionEnabled) {
        this.isSecureConnectionEnabled = isSecureConnectionEnabled;
    }

    /**
     * Get the secure session factory. We can use the SecureSessionFactory to create secure session.
     * The default value is JdkSecureSessionFactory.
     *
     * @return SecureSessionFactory
     */
    public SecureSessionFactory getSecureSessionFactory() {
        return secureSessionFactory;
    }

    /**
     * Set the secure session factory. We can use the SecureSessionFactory to create secure session.
     *
     * @param secureSessionFactory The default value is JdkSecureSessionFactory.
     */
    public void setSecureSessionFactory(SecureSessionFactory secureSessionFactory) {
        this.secureSessionFactory = secureSessionFactory;
    }
}
