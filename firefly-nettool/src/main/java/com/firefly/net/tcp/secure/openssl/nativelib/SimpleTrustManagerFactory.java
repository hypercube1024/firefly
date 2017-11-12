package com.firefly.net.tcp.secure.openssl.nativelib;


import javax.net.ssl.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;

/**
 * Helps to implement a custom {@link TrustManagerFactory}.
 */
public abstract class SimpleTrustManagerFactory extends TrustManagerFactory {

    private static final Provider PROVIDER = new Provider("", 0.0, "") {
        private static final long serialVersionUID = -2680540247105807895L;
    };

    /**
     * {@link SimpleTrustManagerFactorySpi} must have a reference to {@link SimpleTrustManagerFactory}
     * to delegate its callbacks back to {@link SimpleTrustManagerFactory}.  However, it is impossible to do so,
     * because {@link TrustManagerFactory} requires {@link TrustManagerFactorySpi} at construction time and
     * does not provide a way to access it later.
     * <p>
     * To work around this issue, we use an ugly hack which uses a {@link ThreadLocal}.
     */
    private static final ThreadLocal<SimpleTrustManagerFactorySpi> CURRENT_SPI = ThreadLocal.withInitial(SimpleTrustManagerFactorySpi::new);

    /**
     * Creates a new instance.
     */
    protected SimpleTrustManagerFactory() {
        this("");
    }

    /**
     * Creates a new instance.
     *
     * @param name the name of this {@link TrustManagerFactory}
     */
    protected SimpleTrustManagerFactory(String name) {
        super(CURRENT_SPI.get(), PROVIDER, name);
        CURRENT_SPI.get().init(this);
        CURRENT_SPI.remove();

        if (name == null) {
            throw new NullPointerException("name");
        }
    }

    /**
     * Initializes this factory with a source of certificate authorities and related trust material.
     *
     * @see TrustManagerFactorySpi#engineInit(KeyStore)
     */
    protected abstract void engineInit(KeyStore keyStore) throws Exception;

    /**
     * Initializes this factory with a source of provider-specific key material.
     *
     * @see TrustManagerFactorySpi#engineInit(ManagerFactoryParameters)
     */
    protected abstract void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception;

    /**
     * Returns one trust manager for each type of trust material.
     *
     * @see TrustManagerFactorySpi#engineGetTrustManagers()
     */
    protected abstract TrustManager[] engineGetTrustManagers();

    static final class SimpleTrustManagerFactorySpi extends TrustManagerFactorySpi {

        private SimpleTrustManagerFactory parent;
        private volatile TrustManager[] trustManagers;

        void init(SimpleTrustManagerFactory parent) {
            this.parent = parent;
        }

        @Override
        protected void engineInit(KeyStore keyStore) throws KeyStoreException {
            try {
                parent.engineInit(keyStore);
            } catch (KeyStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new KeyStoreException(e);
            }
        }

        @Override
        protected void engineInit(
                ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
            try {
                parent.engineInit(managerFactoryParameters);
            } catch (InvalidAlgorithmParameterException e) {
                throw e;
            } catch (Exception e) {
                throw new InvalidAlgorithmParameterException(e);
            }
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            TrustManager[] trustManagers = this.trustManagers;
            if (trustManagers == null) {
                trustManagers = parent.engineGetTrustManagers();
                if (PlatformDependent.javaVersion() >= 7) {
                    for (int i = 0; i < trustManagers.length; i++) {
                        final TrustManager tm = trustManagers[i];
                        if (tm instanceof X509TrustManager && !(tm instanceof X509ExtendedTrustManager)) {
                            trustManagers[i] = new X509TrustManagerWrapper((X509TrustManager) tm);
                        }
                    }
                }
                this.trustManagers = trustManagers;
            }
            return trustManagers.clone();
        }
    }
}
