package com.firefly.client.websocket;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.UpgradeRequestAdapter;
import com.firefly.utils.StringUtils;
import com.firefly.utils.codec.B64Code;
import com.firefly.utils.collection.MultiMap;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Allowing a generate from a UpgradeRequest
 */
public class ClientUpgradeRequest extends UpgradeRequestAdapter {

    private static final Set<String> FORBIDDEN_HEADERS;

    static {
        // Headers not allowed to be set in ClientUpgradeRequest.headers.
        FORBIDDEN_HEADERS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // Cookies are handled explicitly, avoid to add them twice.
        FORBIDDEN_HEADERS.add("cookie");
        // Headers that cannot be set by applications.
        FORBIDDEN_HEADERS.add("upgrade");
        FORBIDDEN_HEADERS.add("host");
        FORBIDDEN_HEADERS.add("connection");
        FORBIDDEN_HEADERS.add("sec-websocket-key");
        FORBIDDEN_HEADERS.add("sec-websocket-extensions");
        FORBIDDEN_HEADERS.add("sec-websocket-accept");
        FORBIDDEN_HEADERS.add("sec-websocket-protocol");
        FORBIDDEN_HEADERS.add("sec-websocket-version");
        FORBIDDEN_HEADERS.add("pragma");
        FORBIDDEN_HEADERS.add("cache-control");
    }

    private final String key;
    private Object localEndpoint;

    public ClientUpgradeRequest() {
        super();
        this.key = genRandomKey();
    }

    public ClientUpgradeRequest(HttpURI requestURI) {
        super(requestURI);
        this.key = genRandomKey();
    }

    public ClientUpgradeRequest(MetaData.Request wsRequest) {
        this(wsRequest.getURI());
        // headers
        Map<String, List<String>> headers = new HashMap<>();
        HttpFields fields = wsRequest.getFields();
        for (HttpField field : fields) {
            String key = field.getName();
            List<String> values = headers.get(key);
            if (values == null) {
                values = new ArrayList<>();
            }
            values.addAll(Arrays.asList(field.getValues()));
            headers.put(key, values);
            // sub protocols
            if (key.equalsIgnoreCase("Sec-WebSocket-Protocol")) {
                for (String subProtocol : field.getValue().split(",")) {
                    setSubProtocols(subProtocol);
                }
            }
            // extensions
            if (key.equalsIgnoreCase("Sec-WebSocket-Extensions")) {
                for (ExtensionConfig ext : ExtensionConfig.parseList(field.getValues())) {
                    addExtensions(ext);
                }
            }
        }
        super.setHeaders(headers);
        // sessions
        setHttpVersion(wsRequest.getHttpVersion().toString());
        setMethod(wsRequest.getMethod());
    }

    private String genRandomKey() {
        byte[] bytes = new byte[16];
        ThreadLocalRandom.current().nextBytes(bytes);
        return new String(B64Code.encode(bytes));
    }

    public String getKey() {
        return key;
    }

    @Override
    public void setRequestURI(HttpURI uri) {
        super.setRequestURI(uri);

        // parse parameter map
        Map<String, List<String>> pmap = new HashMap<>();

        String query = uri.getQuery();

        if (StringUtils.hasText(query)) {
            MultiMap<String> params = new MultiMap<>();
            UrlEncoded.decodeTo(uri.getQuery(), params, StandardCharsets.UTF_8);

            for (String key : params.keySet()) {
                List<String> values = params.getValues(key);
                if (values == null) {
                    pmap.put(key, new ArrayList<>());
                } else {
                    // break link to original
                    List<String> copy = new ArrayList<>(values);
                    pmap.put(key, copy);
                }
            }

            super.setParameterMap(pmap);
        }
    }

    public void setLocalEndpoint(Object websocket) {
        this.localEndpoint = websocket;
    }

    public Object getLocalEndpoint() {
        return localEndpoint;
    }
}
