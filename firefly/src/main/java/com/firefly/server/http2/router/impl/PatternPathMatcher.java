package com.firefly.server.http2.router.impl;

/**
 * @author Pengtao Qiu
 */
public class PatternPathMatcher extends AbstractPatternMatcher {

    @Override
    public MatchType getMatchType() {
        return MatchType.PATH;
    }

}
