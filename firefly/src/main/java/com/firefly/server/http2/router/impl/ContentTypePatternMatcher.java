package com.firefly.server.http2.router.impl;

/**
 * @author Pengtao Qiu
 */
public class ContentTypePatternMatcher extends AbstractPatternMatcher {

    @Override
    public MatchType getMatchType() {
        return MatchType.CONTENT_TYPE;
    }

}
