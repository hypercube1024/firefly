package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractPreciseMatcher implements Matcher {

    protected Map<String, Set<Router>> map;

    private Map<String, Set<Router>> map() {
        if (map == null) {
            map = new HashMap<>();
        }
        return map;
    }

    @Override
    public void add(String rule, Router router) {
        map().computeIfAbsent(rule, k -> new HashSet<>()).add(router);
    }

    @Override
    public MatchResult match(String value) {
        if (map == null) {
            return null;
        }

        Set<Router> routers = map.get(value);
        if (routers != null && !routers.isEmpty()) {
            return new MatchResult(routers, Collections.emptyMap(), getMatchType());
        } else {
            return null;
        }
    }

}
