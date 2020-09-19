package com.fireflysource.net.tcp.secure;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.JavaVersion;
import com.fireflysource.net.tcp.secure.conscrypt.NoCheckConscryptSSLContextFactory;
import com.fireflysource.net.tcp.secure.conscrypt.SelfSignedCertificateConscryptSSLContextFactory;
import com.fireflysource.net.tcp.secure.jdk.NoCheckOpenJdkSSLContextFactory;
import com.fireflysource.net.tcp.secure.jdk.SelfSignedCertificateOpenJdkSSLContextFactory;

public class DefaultSecureEngineFactorySelector {

    public static SecureEngineFactory createSecureEngineFactory(boolean client) {
        SecureEngineFactory secureEngineFactory;
        if (JavaVersion.VERSION.getPlatform() < 9) {
            if (JavaVersion.VERSION.getPlatform() == 8) {
                String[] update = StringUtils.split(JavaVersion.VERSION.getVersion(), '_');
                if (update.length == 2) {
                    try {
                        int u = Integer.parseInt(update[1]);
                        if (u >= 252) {
                            secureEngineFactory = createOpenJdkSecureEngineFactory(client);
                        } else {
                            secureEngineFactory = createConscryptSecureEngineFactory(client);
                        }
                    } catch (Exception e) {
                        secureEngineFactory = createConscryptSecureEngineFactory(client);
                    }
                } else {
                    secureEngineFactory = createConscryptSecureEngineFactory(client);
                }
            } else {
                secureEngineFactory = createConscryptSecureEngineFactory(client);
            }
        } else {
            secureEngineFactory = createOpenJdkSecureEngineFactory(client);
        }
        return secureEngineFactory;
    }

    private static SecureEngineFactory createOpenJdkSecureEngineFactory(boolean client) {
        SecureEngineFactory secureEngineFactory;
        if (client) {
            secureEngineFactory = new NoCheckOpenJdkSSLContextFactory();
        } else {
            secureEngineFactory = new SelfSignedCertificateOpenJdkSSLContextFactory();
        }
        return secureEngineFactory;
    }

    private static SecureEngineFactory createConscryptSecureEngineFactory(boolean client) {
        SecureEngineFactory secureEngineFactory;
        if (client) {
            secureEngineFactory = new NoCheckConscryptSSLContextFactory();
        } else {
            secureEngineFactory = new SelfSignedCertificateConscryptSSLContextFactory();
        }
        return secureEngineFactory;
    }
}
