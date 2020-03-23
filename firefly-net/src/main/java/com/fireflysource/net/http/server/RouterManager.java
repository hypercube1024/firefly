package com.fireflysource.net.http.server;

import java.util.*;

public interface RouterManager {

    class RouterMatchResult implements Comparable<RouterMatchResult> {

        private final Router router;
        private final Map<String, String> parameters;
        private final Set<Matcher.MatchType> matchTypes;

        public RouterMatchResult(Router router, Map<String, String> parameters, Set<Matcher.MatchType> matchTypes) {
            this.router = router;
            this.parameters = Collections.unmodifiableMap(parameters);
            this.matchTypes = Collections.unmodifiableSet(matchTypes);
        }

        public Router getRouter() {
            return router;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public Set<Matcher.MatchType> getMatchTypes() {
            return matchTypes;
        }

        @Override
        public int compareTo(RouterMatchResult o) {
            return router.compareTo(o.getRouter());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RouterMatchResult that = (RouterMatchResult) o;
            return Objects.equals(router, that.router);
        }

        @Override
        public int hashCode() {
            return Objects.hash(router);
        }
    }

    /**
     * Register a router using automatic increase id.
     *
     * @return The new router.
     */
    Router register();

    /**
     * Register a router.
     *
     * @param id The router id.
     * @return The new router.
     */
    Router register(Integer id);

    /**
     * Find routers.
     *
     * @param method      The HTTP method.
     * @param path        The path.
     * @param contentType Content type.
     * @param accept      Accepted content type.
     * @return The registered routers.
     */
    NavigableSet<RouterMatchResult> findRouter(String method, String path, String contentType, String accept);
}
