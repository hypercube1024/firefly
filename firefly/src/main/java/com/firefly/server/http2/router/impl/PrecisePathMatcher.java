package com.firefly.server.http2.router.impl;

/**
 * @author Pengtao Qiu
 */
public class PrecisePathMatcher extends AbstractPreciseMatcher {


    @Override
    public MatchResult match(String value) {
        if (value.charAt(value.length() - 1) != '/') {
            value += "/";
        }

        return super.match(value);
    }

    @Override
    public MatchType getMatchType() {
        return MatchType.PATH;
    }
}
