package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.utils.StringUtils;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class PrecisePathMatcher implements Matcher {

    private Map<String, Set<Router>> precisePath;

    private Map<String, Set<Router>> precisePath() {
        if (precisePath == null) {
            precisePath = new HashMap<>();
        }
        return precisePath;
    }

    @Override
    public void add(String rule, Router router) {
        precisePath().computeIfAbsent(rule, k -> new HashSet<>()).add(router);
    }

    @Override
    public MatchResult match(String value) {
        if (precisePath == null) {
            return null;
        }

        if (!StringUtils.hasText(value)) {
            return null;
        }

        if (value.charAt(value.length() - 1) != '/') {
            value += "/";
        }

        Set<Router> routers = precisePath.get(value);
        if (routers != null) {
            return new MatchResult(routers, Collections.emptyMap(), MatchType.PATH);
        } else {
            return null;
        }
    }
}
