package com.firefly.net.tcp.secure;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.utils.CollectionUtils;
import org.eclipse.jetty.alpn.ALPN;

import javax.net.ssl.SSLEngine;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class ALPNSelector implements ApplicationProtocolSelector {

    private volatile String applicationProtocol;
    private List<String> supportedProtocols = Arrays.asList("h2", "http/1.1");

    public ALPNSelector(SSLEngine sslEngine) {
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
                    ALPN.remove(sslEngine);
                    if (CollectionUtils.isEmpty(protocols)) {
                        for (String p : supportedProtocols) {
                            if (protocols.contains(p)) {
                                applicationProtocol = p;
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
    public String applicationProtocol() {
        return applicationProtocol;
    }

}
