package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher

class PathPatternMatcher : AbstractPatternMatcher() {

    override fun getMatchType(): Matcher.MatchType {
        return Matcher.MatchType.PATH
    }
}