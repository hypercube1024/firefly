package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.common.collection.CollectionUtils;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.CommonLogger;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import org.conscrypt.Conscrypt;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class ConscryptApplicationProtocolSelector implements ApplicationProtocolSelector {

    private static final CommonLogger log = CommonLogger.create(ConscryptApplicationProtocolSelector.class);

    private final String[] supportedProtocols;
    private final List<String> supportedProtocolList;
    private final SSLEngine sslEngine;

    public ConscryptApplicationProtocolSelector(SSLEngine sslEngine, List<String> supportedProtocolList) {
        if (CollectionUtils.isEmpty(supportedProtocolList)) {
            this.supportedProtocolList = Collections.unmodifiableList(Arrays.asList("h2", "http/1.1"));
        } else {
            this.supportedProtocolList = supportedProtocolList;
        }
        supportedProtocols = this.supportedProtocolList.toArray(StringUtils.EMPTY_STRING_ARRAY);
        this.sslEngine = sslEngine;
        if (sslEngine.getUseClientMode()) {
            Conscrypt.setApplicationProtocols(sslEngine, supportedProtocols);
        } else {
            Conscrypt.setApplicationProtocolSelector(sslEngine, new Selector());
        }
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
            if (!CollectionUtils.isEmpty(clientProtocols)) {
                for (String p : supportedProtocols) {
                    if (clientProtocols.contains(p)) {
                        log.debug(() -> "ALPN local server selected protocol -> " + p);
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
