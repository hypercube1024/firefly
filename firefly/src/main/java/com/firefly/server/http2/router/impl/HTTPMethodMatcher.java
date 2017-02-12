package com.firefly.server.http2.router.impl;

import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class HTTPMethodMatcher implements Matcher {

    private Map<HttpMethod, Set<Router>> httpMethod;

    private Map<HttpMethod, Set<Router>> httpMethod() {
        if (httpMethod == null) {
            httpMethod = new HashMap<>();
        }
        return httpMethod;
    }

    @Override
    public void add(String rule, Router router) {
        HttpMethod method = HttpMethod.fromString(rule.toUpperCase());
        if (method == null) {
            throw new IllegalArgumentException("not support this method [" + rule + "]");
        }

        httpMethod().computeIfAbsent(method, k -> new HashSet<>()).add(router);
    }

    @Override
    public MatchResult match(String value) {
        if (httpMethod == null) {
            return null;
        }

        HttpMethod method = HttpMethod.fromString(value.toUpperCase());
        if (method == null) {
            return null;
        }

        Set<Router> routers = httpMethod.get(method);
        if (routers != null && !routers.isEmpty()) {
            return new MatchResult(routers, Collections.emptyMap(), MatchType.METHOD);
        } else {
            return null;
        }
    }
}
