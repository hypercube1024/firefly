package com.firefly.net.tcp;

import com.firefly.net.Config;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.tcp.secure.JdkSecureSessionFactory;

public class TcpConfiguration extends Config {

    // SSL/TLS settings
    private boolean isSecureConnectionEnabled;
    private SecureSessionFactory secureSessionFactory = new JdkSecureSessionFactory();

    public boolean isSecureConnectionEnabled() {
        return isSecureConnectionEnabled;
    }

    public void setSecureConnectionEnabled(boolean isSecureConnectionEnabled) {
        this.isSecureConnectionEnabled = isSecureConnectionEnabled;
    }

    public SecureSessionFactory getSecureSessionFactory() {
        return secureSessionFactory;
    }

    public void setSecureSessionFactory(SecureSessionFactory secureSessionFactory) {
        this.secureSessionFactory = secureSessionFactory;
    }
}
