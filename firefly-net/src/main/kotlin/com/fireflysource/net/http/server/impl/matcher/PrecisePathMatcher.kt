package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Matcher.MatchType
import com.fireflysource.net.http.server.Router

class PrecisePathMatcher : AbstractPreciseMatcher() {

    override fun add(rule: String, router: Router) {
        val path = toPath(rule)
        super.add(path, router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        if (routersMap.isEmpty()) return null

        val path = toPath(value)
        return super.match(path)
    }

    override fun getMatchType(): MatchType? {
        return MatchType.PATH
    }

    private fun toPath(value: String): String = if (value.last() != '/') "$value/" else value
}