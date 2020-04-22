package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router

class HttpMethodMatcher : AbstractPreciseMatcher() {

    override fun add(rule: String, router: Router) {
        super.add(rule.toUpperCase(), router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        return if (map.isEmpty()) null else super.match(value.toUpperCase())
    }

    override fun getMatchType(): Matcher.MatchType {
        return Matcher.MatchType.METHOD
    }

}