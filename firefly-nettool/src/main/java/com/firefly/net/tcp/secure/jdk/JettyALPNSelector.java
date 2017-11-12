package com.firefly.net.tcp.secure.jdk;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.utils.CollectionUtils;
import org.eclipse.jetty.alpn.ALPN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class JettyALPNSelector implements ApplicationProtocolSelector {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    private volatile String applicationProtocol;
    private final List<String> supportedProtocols = Arrays.asList("h2", "http/1.1");

    public JettyALPNSelector(SSLEngine sslEngine) {
        if (sslEngine.getUseClientMode()) {
            ALPN.put(sslEngine, new ALPN.ClientProvider() {

                @Override
                public List<String> protocols() {
                    return supportedProtocols;
                }

                @Override
                public void unsupported() {
                    ALPN.remove(sslEngine);
                }

                @Override
                public void selected(String protocol) {
                    ALPN.remove(sslEngine);
                    log.debug("ALPN remote server selected protocol -> {}", protocol);
                    applicationProtocol = protocol;
                }
            });
        } else {
            ALPN.put(sslEngine, new ALPN.ServerProvider() {
                @Override
                public void unsupported() {
                    ALPN.remove(sslEngine);
                }

                @Override
                public String select(List<String> protocols) {
                    log.debug("ALPN remote client supported protocols -> {}", protocols);
                    ALPN.remove(sslEngine);
                    if (!CollectionUtils.isEmpty(protocols)) {
                        for (String p : supportedProtocols) {
                            if (protocols.contains(p)) {
                                applicationProtocol = p;
                                log.debug("ALPN local server selected protocol -> {}", p);
                                return applicationProtocol;
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    @Override
    public String getApplicationProtocol() {
        return applicationProtocol;
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return supportedProtocols;
    }
}
