package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Pengtao Qiu
 */
public class RegexPathMatcher implements Matcher {

    private Map<RegexPath, Set<Router>> regexPath;

    private static class RegexPath {
        final String rule;
        final Pattern pattern;

        public RegexPath(String rule) {
            this.rule = rule;
            pattern = Pattern.compile(rule);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegexPath regexPath = (RegexPath) o;
            return Objects.equals(rule, regexPath.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule);
        }
    }

    private Map<RegexPath, Set<Router>> regexPath() {
        if (regexPath == null) {
            regexPath = new HashMap<>();
        }
        return regexPath;
    }

    @Override
    public void add(String rule, Router router) {
        regexPath().computeIfAbsent(new RegexPath(rule), k -> new HashSet<>()).add(router);
    }

    @Override
    public MatchResult match(String value) {
        if (regexPath == null) {
            return null;
        }

        Set<Router> routers = new HashSet<>();
        Map<Router, Map<String, String>> parameters = new HashMap<>();

        regexPath.entrySet()
                 .forEach(e -> {
                     java.util.regex.Matcher m = e.getKey().pattern.matcher(value);
                     if (m.matches()) {
                         routers.addAll(e.getValue());
                         m = e.getKey().pattern.matcher(value);

                         Map<String, String> param = new HashMap<>();
                         while (m.find()) {
                             for (int i = 1; i <= m.groupCount(); i++) {
                                 param.put("group" + i, m.group(i));
                             }
                         }
                         if (!param.isEmpty()) {
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
