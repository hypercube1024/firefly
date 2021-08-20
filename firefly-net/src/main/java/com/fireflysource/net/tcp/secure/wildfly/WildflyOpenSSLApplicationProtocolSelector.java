package com.fireflysource.net.tcp.secure.wildfly;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;

import javax.net.ssl.SSLEngine;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;

public class WildflyOpenSSLApplicationProtocolSelector implements ApplicationProtocolSelector {

    private static final LazyLogger LOG = SystemLogger.create(WildflyOpenSSLApplicationProtocolSelector.class);
    private static Method setApplicationProtocols;
    private static Method setHandshakeApplicationProtocolSelector;
    private static Method getApplicationProtocol;

    private final SSLEngine sslEngine;
    private final List<String> supportedProtocolList;

    static {
        try {
            Class<?> sslEngineImpl = Class.forName("org.wildfly.openssl.OpenSSLEngine");
            setApplicationProtocols = sslEngineImpl.getMethod("setApplicationProtocols", String[].class);
            setHandshakeApplicationProtocolSelector = sslEngineImpl.getMethod("setHandshakeApplicationProtocolSelector", BiFunction.class);
            getApplicationProtocol = sslEngineImpl.getMethod("getApplicationProtocol");
            setApplicationProtocols.setAccessible(true);
            setHandshakeApplicationProtocolSelector.setAccessible(true);
            getApplicationProtocol.setAccessible(true);
        } catch (Exception e) {
            LOG.error("Get wildfly openssl application protocol selector methods exception.", e);
        }
    }

    public WildflyOpenSSLApplicationProtocolSelector(SSLEngine sslEngine, List<String> supportedProtocolList) {
        this.sslEngine = sslEngine;
        this.supportedProtocolList = supportedProtocolList;

        try {
            if (sslEngine.getUseClientMode()) {
                setApplicationProtocols.invoke(sslEngine, new Object[]{supportedProtocolList.toArray(StringUtils.EMPTY_STRING_ARRAY)});
            } else {
                BiFunction<SSLEngine, List<String>, String> selector = (serverEngine, clientProtocols) -> {
                    if (clientProtocols != null) {
                        for (String p : this.supportedProtocolList) {
                            if (clientProtocols.contains(p)) {
                                LOG.debug(() -> "ALPN local server selected protocol -> " + p);
                                return p;
                            }
                        }
                    }
                    return null;
                };
                setHandshakeApplicationProtocolSelector.invoke(sslEngine, selector);
            }
        } catch (Exception e) {
            LOG.error("Init wildfly openssl application protocol selector exception.", e);
        }
    }

    @Override
    public String getApplicationProtocol() {
        String protocol;
        try {
            protocol = (String) getApplicationProtocol.invoke(sslEngine);
        } catch (Exception e) {
            LOG.error("Get application protocol exception", e);
            protocol = null;
        }
        return protocol;
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return supportedProtocolList;
    }
}
