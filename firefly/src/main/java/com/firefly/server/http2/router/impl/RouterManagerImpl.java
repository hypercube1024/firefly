package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.RouterManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class RouterManagerImpl implements RouterManager {

    private AtomicInteger idGenerator = new AtomicInteger();
    private final Matcher precisePathMather = new PrecisePathMatcher();
    private final Matcher patternPathMatcher = new PatternPathMatcher();
    private final Matcher regexPathMatcher = new RegexPathMatcher();
    private final Matcher parameterPathMatcher = new ParameterPathMatcher();
    private final Matcher httpMethodMatcher = new HTTPMethodMatcher();
    private final Matcher contentTypePreciseMatcher = new ContentTypePreciseMatcher();
    private final Matcher acceptHeaderPreciseMatcher = new AcceptHeaderPreciseMatcher();
    private final Matcher contentTypePatternMatcher = new ContentTypePatternMatcher();
    private final Matcher acceptHeaderPatternMatcher = new AcceptHeaderPatternMatcher();

    public static class RouterMatchResult {

        private final Router router;
        private final Map<String, String> parameters;
        private final Set<Matcher.MatchType> matchTypes;

        public RouterMatchResult(Router router, Map<String, String> parameters, Set<Matcher.MatchType> matchTypes) {
            this.router = router;
            this.parameters = parameters;
            this.matchTypes = matchTypes;
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
    }

    public Matcher getHttpMethodMatcher() {
        return httpMethodMatcher;
    }

    public Matcher getPrecisePathMather() {
        return precisePathMather;
    }

    public Matcher getPatternPathMatcher() {
        return patternPathMatcher;
    }

    public Matcher getRegexPathMatcher() {
        return regexPathMatcher;
    }

    public Matcher getParameterPathMatcher() {
        return parameterPathMatcher;
    }

    public Matcher getContentTypePreciseMatcher() {
        return contentTypePreciseMatcher;
    }

    public Matcher getAcceptHeaderPreciseMatcher() {
        return acceptHeaderPreciseMatcher;
    }

    public Matcher getContentTypePatternMatcher() {
        return contentTypePatternMatcher;
    }

    public Matcher getAcceptHeaderPatternMatcher() {
        return acceptHeaderPatternMatcher;
    }

    public List<RouterMatchResult> findRouter(String method, String url, String contentType, String accept) {
        // TODO
        return null;
    }

    @Override
    public Router register() {
        return new RouterImpl(idGenerator.getAndIncrement(), this);
    }

    @Override
    public void accept(SimpleRequest request) {

    }
}
