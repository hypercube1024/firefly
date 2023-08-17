package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import java.util.*

abstract class AbstractPreciseMatcher : AbstractMatcher<String>(), Matcher {

    override fun add(rule: String, router: Router) {
        routersMap.computeIfAbsent(rule) { TreeSet() }.add(router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        val routers = routersMap[value]
        return if (!routers.isNullOrEmpty()) {
            Matcher.MatchResult(routers, emptyMap(), matchType)
        } else null
    }
}