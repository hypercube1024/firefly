package com.firefly.server.http2.router.impl;


import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.utils.pattern.Pattern;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractPatternMatcher implements Matcher {

    protected Map<PatternRule, Set<Router>> patternMap;

    protected static class PatternRule {
        final String rule;
        final Pattern pattern;

        protected PatternRule(String rule) {
            this.rule = rule;
            pattern = Pattern.compile(rule, "*");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PatternRule that = (PatternRule) o;
            return Objects.equals(rule, that.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule);
        }
    }

    protected Map<PatternRule, Set<Router>> patternMap() {
        if (patternMap == null) {
            patternMap = new HashMap<>();
        }
        return patternMap;
    }

    @Override
    public void add(String rule, Router router) {
        patternMap().computeIfAbsent(new PatternRule(rule), k -> new HashSet<>()).add(router);
    }

    @Override
    public MatchResult match(String v) {
        if (patternMap == null) {
            return null;
        }

        Set<Router> routers = new HashSet<>();
        Map<Router, Map<String, String>> parameters = new HashMap<>();

        patternMap.forEach((rule, routerSet) -> {
            String[] strings = rule.pattern.match(v);
            if (strings != null) {
                routers.addAll(routerSet);
                if (strings.length > 0) {
                    Map<String, String> param = new HashMap<>();
                    for (int i = 0; i < strings.length; i++) {
                        param.put("param" + i, strings[i]);
                    }
                    routerSet.forEach(router -> parameters.put(router, param));
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
