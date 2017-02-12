package com.firefly.server.http2.router.impl;

/**
 * @author Pengtao Qiu
 */
public class RegexPathMatcher extends AbstractRegexMatcher {
    
    @Override
    public MatchType getMatchType() {
        return MatchType.PATH;
    }

}
