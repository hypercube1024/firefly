package com.firefly.server.http2.router.impl;

/**
 * @author Pengtao Qiu
 */
public class AcceptHeaderPatternMatcher extends AbstractPatternMatcher {

    @Override
    public MatchType getMatchType() {
        return MatchType.ACCEPT;
    }

}
