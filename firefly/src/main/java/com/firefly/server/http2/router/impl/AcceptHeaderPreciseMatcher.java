package com.firefly.server.http2.router.impl;

/**
 * @author Pengtao Qiu
 */
public class AcceptHeaderPreciseMatcher extends AbstractPreciseMatcher {

    @Override
    public MatchType getMatchType() {
        return MatchType.ACCEPT;
    }

}
