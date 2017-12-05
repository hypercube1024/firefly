package com.firefly.net.tcp.secure.conscrypt;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.utils.CollectionUtils;
import org.conscrypt.Conscrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class ConscryptALPNSelector implements ApplicationProtocolSelector {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");
    private static final String[] supportedProtocols = {"http/1.1"};
    private static final List<String> supportedProtocolList = Collections.unmodifiableList(Arrays.asList(supportedProtocols));

    private final SSLEngine sslEngine;

    public ConscryptALPNSelector(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
        if (sslEngine.getUseClientMode()) {
            Conscrypt.setApplicationProtocols(sslEngine, supportedProtocols);
        } else {
            Conscrypt.setApplicationProtocolSelector(sslEngine, new ConscryptApplicationProtocolSelector());
        }
    }

    private final class ConscryptApplicationProtocolSelector extends org.conscrypt.ApplicationProtocolSelector {

        @Override
        public String selectApplicationProtocol(SSLEngine sslEngine, List<String> list) {
            return select(list);
        }

        @Override
        public String selectApplicationProtocol(SSLSocket sslSocket, List<String> list) {
            return select(list);
        }

        public String select(List<String> clientProtocols) {
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
        return Conscrypt.getApplicationProtocol(sslEngine);
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return supportedProtocolList;
    }
}
