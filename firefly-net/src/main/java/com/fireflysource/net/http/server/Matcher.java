package com.fireflysource.net.http.server;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

public interface Matcher {

    enum MatchType {
        PATH, METHOD, ACCEPT, CONTENT_TYPE
    }

    class MatchResult {
        private final SortedSet<Router> routers;
        private final Map<Router, Map<String, String>> parameters;
        private final MatchType matchType;

        public MatchResult(SortedSet<Router> routers, Map<Router, Map<String, String>> parameters, MatchType matchType) {
            this.routers = routers;
            this.parameters = Collections.unmodifiableMap(parameters);
            this.matchType = matchType;
        }

        public SortedSet<Router> getRouters() {
            return routers;
        }

        public Map<Router, Map<String, String>> getParameters() {
            return parameters;
        }

        public MatchType getMatchType() {
            return matchType;
        }
    }

    void add(String rule, Router router);

    MatchResult match(String value);

    MatchType getMatchType();
}
