package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.utils.PathUtils;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class ParameterPathMatcher implements Matcher {

    private Map<Integer, Map<String, Set<Router>>> parameterPath;

    private Map<Integer, Map<String, Set<Router>>> parameterPath() {
        if (parameterPath == null) {
            parameterPath = new HashMap<>();
        }
        return parameterPath;
    }

    @Override
    public void add(String rule, Router router) {
        List<String> paths = PathUtils.split(rule);
        parameterPath().computeIfAbsent(paths.size(), k -> new HashMap<>())
                       .computeIfAbsent(rule, k -> new HashSet<>())
                       .add(router);
    }

    @Override
    public MatchResult match(String rule) {
        // TODO
        return null;
    }
}
