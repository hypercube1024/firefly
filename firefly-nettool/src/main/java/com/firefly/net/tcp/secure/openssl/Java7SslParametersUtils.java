package com.firefly.net.tcp.secure.openssl;

import javax.net.ssl.SSLParameters;
import java.security.AlgorithmConstraints;

final class Java7SslParametersUtils {

    private Java7SslParametersUtils() {
        // Utility
    }

    /**
     * Utility method that is used by {@link OpenSslEngine} and so allow use not not have any reference to
     * {@link AlgorithmConstraints} in the code. This helps us to not get into trouble when using it in java
     * version < 7 and especially when using on android.
     */
    static void setAlgorithmConstraints(SSLParameters sslParameters, Object algorithmConstraints) {
        sslParameters.setAlgorithmConstraints((AlgorithmConstraints) algorithmConstraints);
    }
}
