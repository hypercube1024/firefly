package com.fireflysource.net.http.server;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface Matcher {

    enum MatchType {
        PATH, METHOD, ACCEPT, CONTENT_TYPE
    }

    class MatchResult {
        private final Set<Router> routers;
        private final Map<Router, Map<String, String>> parameters;
        private final MatchType matchType;

        public MatchResult(Set<Router> routers, Map<Router, Map<String, String>> parameters, MatchType matchType) {
            this.routers = routers;
            this.parameters = Collections.unmodifiableMap(parameters);
            this.matchType = matchType;
        }

        public Set<Router> getRouters() {
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
