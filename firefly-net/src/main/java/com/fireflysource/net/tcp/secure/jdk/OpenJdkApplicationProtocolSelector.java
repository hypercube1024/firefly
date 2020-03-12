package com.fireflysource.net.tcp.secure.jdk;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.JavaVersion;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;

public class OpenJdkApplicationProtocolSelector implements ApplicationProtocolSelector {

    private static final LazyLogger LOG = SystemLogger.create(OpenJdkApplicationProtocolSelector.class);
    private static Method setApplicationProtocols;
    private static Method setHandshakeApplicationProtocolSelector;
    private static Method getApplicationProtocol;

    private final String[] supportedProtocols;
    private final List<String> supportedProtocolList;
    private final SSLEngine sslEngine;

    static {
        try {
            if (JavaVersion.VERSION.getPlatform() < 9) {
                setApplicationProtocols = org.openjsse.javax.net.ssl.SSLParameters.class.getMethod("setApplicationProtocols", String[].class);
                Class<?> sslEngineImpl = Class.forName("org.openjsse.sun.security.ssl.SSLEngineImpl");
                setHandshakeApplicationProtocolSelector = sslEngineImpl.getMethod("setHandshakeApplicationProtocolSelector", BiFunction.class);
                getApplicationProtocol = sslEngineImpl.getMethod("getApplicationProtocol");
            } else {
                setApplicationProtocols = SSLParameters.class.getMethod("setApplicationProtocols", String[].class);
                setHandshakeApplicationProtocolSelector = SSLEngine.class.getMethod("setHandshakeApplicationProtocolSelector", BiFunction.class);
                getApplicationProtocol = SSLEngine.class.getMethod("getApplicationProtocol");
            }
            setApplicationProtocols.setAccessible(true);
            setHandshakeApplicationProtocolSelector.setAccessible(true);
            getApplicationProtocol.setAccessible(true);
        } catch (Exception e) {
            LOG.error("Init openjsse application protocol selector exception", e);
        }
    }

    public OpenJdkApplicationProtocolSelector(SSLEngine sslEngine, List<String> supportedProtocolList) {
        this.supportedProtocolList = supportedProtocolList;
        supportedProtocols = this.supportedProtocolList.toArray(StringUtils.EMPTY_STRING_ARRAY);
        this.sslEngine = sslEngine;

        try {
            if (sslEngine.getUseClientMode()) {
                SSLParameters parameters;
                if (JavaVersion.VERSION.getPlatform() < 9) {
                    parameters = new org.openjsse.javax.net.ssl.SSLParameters();
                } else {
                    parameters = new SSLParameters();
                }
                setApplicationProtocols.invoke(parameters, new Object[]{supportedProtocols});
                sslEngine.setSSLParameters(parameters);

            } else {
                BiFunction<SSLEngine, List<String>, String> selector = (serverEngine, clientProtocols) -> {
                    if (clientProtocols != null) {
                        for (String p : supportedProtocols) {
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
            LOG.error("Init openjsse application protocol selector exception", e);
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
