package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.utils.pattern.Pattern;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class PatternPathMatcher implements Matcher {

    private Map<PatternPath, Set<Router>> patternPath;

    private static class PatternPath {
        final String rule;
        final Pattern pattern;

        public PatternPath(String rule) {
            this.rule = rule;
            pattern = Pattern.compile(rule, "*");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PatternPath that = (PatternPath) o;
            return Objects.equals(rule, that.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule);
        }
    }

    private Map<PatternPath, Set<Router>> patternPath() {
        if (patternPath == null) {
            patternPath = new HashMap<>();
        }
        return patternPath;
    }

    @Override
    public void add(String rule, Router router) {
        patternPath().computeIfAbsent(new PatternPath(rule), k -> new HashSet<>()).add(router);
    }

    @Override
    public MatchResult match(String value) {
        if (patternPath == null) {
            return null;
        }

        Set<Router> routers = new HashSet<>();
        Map<Router, Map<String, String>> parameters = new HashMap<>();

        patternPath.entrySet()
                   .forEach(e -> {
                       String[] strings = e.getKey().pattern.match(value);
                       if (strings != null) {
                           routers.addAll(e.getValue());
                           if (strings.length > 0) {
                               Map<String, String> param = new HashMap<>();
                               for (int i = 0; i < strings.length; i++) {
                                   param.put("param" + i, strings[i]);
                               }
                               e.getValue().forEach(router -> parameters.put(router, param));
                           }
                       }
                   });
        if (routers.isEmpty()) {
            return null;
        } else {
            return new MatchResult(routers, parameters, MatchType.PATH);
        }
    }


}
