package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import java.util.*

class HttpMethodMatcher : AbstractPreciseMatcher() {

    override fun add(rule: String, router: Router) {
        super.add(rule.uppercase(Locale.getDefault()), router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        return if (routersMap.isEmpty()) null else super.match(value.uppercase(Locale.getDefault()))
    }

    override fun getMatchType(): Matcher.MatchType {
        return Matcher.MatchType.METHOD
    }

}