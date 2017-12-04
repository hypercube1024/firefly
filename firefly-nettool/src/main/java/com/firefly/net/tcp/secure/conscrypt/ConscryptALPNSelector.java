package com.firefly.net.tcp.secure.conscrypt;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author Pengtao Qiu
 */
public class ConscryptALPNSelector implements ApplicationProtocolSelector {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");
    private static final String[] supportedProtocols = {"h2", "http/1.1"};
    private static final List<String> supportedProtocolList = Collections.unmodifiableList(Arrays.asList(supportedProtocols));

    private final SSLEngine sslEngine;

    public ConscryptALPNSelector(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
        try {
            if (sslEngine.getUseClientMode()) {
                Method setAlpnProtocols = sslEngine.getClass().getDeclaredMethod("setApplicationProtocols", String[].class);
                setAlpnProtocols.setAccessible(true);
                setAlpnProtocols.invoke(sslEngine, (Object) supportedProtocols);
            } else {
                Method method = sslEngine.getClass().getMethod("setHandshakeApplicationProtocolSelector", BiFunction.class);
                method.setAccessible(true);
                method.invoke(sslEngine, new ALPNCallback());
            }
        } catch (Exception e) {
            log.error("construct Conscrypt ALPN selector exception", e);
        }
    }

    private final class ALPNCallback implements BiFunction<SSLEngine, List<String>, String> {

        @Override
        public String apply(SSLEngine sslEngine, List<String> clientProtocols) {
            if (!CollectionUtils.isEmpty(clientProtocols)) {
                for (String p : supportedProtocols) {
                    if (clientProtocols.contains(p)) {
                        log.debug("ALPN local server selected protocol -> {}", p);
                        return p;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public String getApplicationProtocol() {
        try {
            Method method = sslEngine.getClass().getDeclaredMethod("getApplicationProtocol");
            method.setAccessible(true);
            return (String) method.invoke(sslEngine);
        } catch (Exception e) {
            log.info("get application protocol error. {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return supportedProtocolList;
    }
}
