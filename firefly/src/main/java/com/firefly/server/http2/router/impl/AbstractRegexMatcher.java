package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractRegexMatcher implements Matcher {

    protected Map<RegexRule, Set<Router>> regexMap;

    protected static class RegexRule {
        final String rule;
        final Pattern pattern;

        protected RegexRule(String rule) {
            this.rule = rule;
            pattern = Pattern.compile(rule);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegexRule regexRule = (RegexRule) o;
            return Objects.equals(rule, regexRule.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule);
        }
    }

    protected Map<RegexRule, Set<Router>> regexMap() {
        if (regexMap == null) {
            regexMap = new HashMap<>();
        }
        return regexMap;
    }

    @Override
    public void add(String rule, Router router) {
        regexMap().computeIfAbsent(new RegexRule(rule), k -> new HashSet<>()).add(router);
    }

    @Override
    public MatchResult match(String value) {
        if (regexMap == null) {
            return null;
        }

        Set<Router> routers = new HashSet<>();
        Map<Router, Map<String, String>> parameters = new HashMap<>();

        regexMap.entrySet()
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
            return new MatchResult(routers, parameters, getMatchType());
        }
    }
}
