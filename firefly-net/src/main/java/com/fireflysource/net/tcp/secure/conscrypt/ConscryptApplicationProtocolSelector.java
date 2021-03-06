package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import org.conscrypt.Conscrypt;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import java.util.List;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class ConscryptApplicationProtocolSelector implements ApplicationProtocolSelector {

    private static final LazyLogger LOG = SystemLogger.create(ConscryptApplicationProtocolSelector.class);

    private final String[] supportedProtocols;
    private final List<String> supportedProtocolList;
    private final SSLEngine sslEngine;

    public ConscryptApplicationProtocolSelector(SSLEngine sslEngine, List<String> supportedProtocolList) {
        this.supportedProtocolList = supportedProtocolList;
        supportedProtocols = this.supportedProtocolList.toArray(StringUtils.EMPTY_STRING_ARRAY);
        this.sslEngine = sslEngine;
        if (sslEngine.getUseClientMode()) {
            Conscrypt.setApplicationProtocols(sslEngine, supportedProtocols);
        } else {
            Conscrypt.setApplicationProtocolSelector(sslEngine, new Selector());
        }
    }

    @Override
    public String getApplicationProtocol() {
        return Optional.ofNullable(Conscrypt.getApplicationProtocol(sslEngine)).orElse("");
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return supportedProtocolList;
    }

    private final class Selector extends org.conscrypt.ApplicationProtocolSelector {

        @Override
        public String selectApplicationProtocol(SSLEngine sslEngine, List<String> list) {
            return select(list);
        }

        @Override
        public String selectApplicationProtocol(SSLSocket sslSocket, List<String> list) {
            return select(list);
        }

        String select(List<String> clientProtocols) {
            if (clientProtocols != null) {
                for (String p : supportedProtocols) {
                    if (clientProtocols.contains(p)) {
                        LOG.debug(() -> "ALPN local server selected protocol -> " + p);
                        return p;
                    }
                }
            }
            return null;
        }
    }
}
