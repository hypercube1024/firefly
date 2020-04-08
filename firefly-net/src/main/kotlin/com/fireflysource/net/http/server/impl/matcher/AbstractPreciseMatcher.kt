package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import java.util.*
import kotlin.collections.HashMap


abstract class AbstractPreciseMatcher : Matcher {

    protected val map: MutableMap<String, NavigableSet<Router>> by lazy { HashMap<String, NavigableSet<Router>>() }

    override fun add(rule: String, router: Router) {
        map.computeIfAbsent(rule) { TreeSet() }.add(router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        val routers = map[value]
        return if (routers != null && routers.isNotEmpty()) {
            Matcher.MatchResult(routers, emptyMap(), matchType)
        } else null
    }
}