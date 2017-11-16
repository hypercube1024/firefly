package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.utils.PathUtils;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class ParameterPathMatcher implements Matcher {

    private Map<Integer, Map<ParameterPath, Set<Router>>> parameterPath;

    private static class ParameterPath {
        final String rule;
        final List<String> paths;

        public ParameterPath(String rule) {
            this.rule = rule;
            paths = PathUtils.split(rule);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParameterPath that = (ParameterPath) o;
            return Objects.equals(rule, that.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule);
        }

        Map<String, String> match(List<String> list) {
            Map<String, String> param = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                String path = paths.get(i);
                String value = list.get(i);

                if (path.charAt(0) != ':') {
                    if (!path.equals(value)) {
                        return null;
                    }
                } else {
                    param.put(path.substring(1), value);
                }
            }
            return param;
        }
    }

    private Map<Integer, Map<ParameterPath, Set<Router>>> parameterPath() {
        if (parameterPath == null) {
            parameterPath = new HashMap<>();
        }
        return parameterPath;
    }

    @Override
    public void add(String rule, Router router) {
        ParameterPath parameterPath = new ParameterPath(rule);
        parameterPath().computeIfAbsent(parameterPath.paths.size(), k -> new HashMap<>())
                       .computeIfAbsent(parameterPath, k -> new HashSet<>())
                       .add(router);
    }

    @Override
    public MatchResult match(String value) {
        if (parameterPath == null) {
            return null;
        }

        if (value.length() == 1) {
            if (value.charAt(0) == '/') {
                return null;
            } else {
                throw new IllegalArgumentException("the path: [" + value + "] format error");
            }
        } else {
            List<String> list = PathUtils.split(value);
            Map<ParameterPath, Set<Router>> map = parameterPath.get(list.size());
            if (map != null && !map.isEmpty()) {
                Set<Router> routers = new HashSet<>();
                Map<Router, Map<String, String>> parameters = new HashMap<>();

                map.forEach((key, regRouter) -> {
                    Map<String, String> param = key.match(list);
                    if (param != null) {
                        routers.addAll(regRouter);
                        regRouter.forEach(router -> parameters.put(router, param));
                    }
                });

                if (!routers.isEmpty()) {
                    return new MatchResult(routers, parameters, getMatchType());
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public MatchType getMatchType() {
        return MatchType.PATH;
    }
}
