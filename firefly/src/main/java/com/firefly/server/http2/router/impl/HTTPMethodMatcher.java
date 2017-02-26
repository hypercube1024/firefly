package com.firefly.server.http2.router.impl;

/**
 * @author Pengtao Qiu
 */
public class HTTPMethodMatcher extends AbstractPreciseMatcher {

    @Override
    public MatchResult match(String value) {
        return super.match(value.toUpperCase());
    }

    @Override
    public MatchType getMatchType() {
        return MatchType.METHOD;
    }
}
