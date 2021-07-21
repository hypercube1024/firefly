package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.common.string.StringUtils
import com.fireflysource.net.http.common.model.AcceptMIMEMatchType
import com.fireflysource.net.http.common.model.AcceptMIMEType
import com.fireflysource.net.http.common.model.MimeTypes.parseAcceptMIMETypes
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Matcher.MatchType
import com.fireflysource.net.http.server.Router
import java.util.*


class AcceptHeaderMatcher : AbstractPreciseMatcher() {

    override fun getMatchType(): MatchType? {
        return MatchType.ACCEPT
    }

    override fun match(value: String): Matcher.MatchResult? {
        if (routersMap.isEmpty()) return null

        val acceptMIMETypes = parseAcceptMIMETypes(value)
        if (acceptMIMETypes.isNullOrEmpty()) return null

        return acceptMIMETypes
            .map { findRouters(it) }
            .filter { !it.isNullOrEmpty() }
            .map { Matcher.MatchResult(it, emptyMap(), matchType) }
            .firstOrNull()
    }

    private fun findRouters(type: AcceptMIMEType) =
        routersMap.entries.filter { matchMimeType(it, type) }.flatMap { it.value }.toSortedSet()

    private fun matchMimeType(e: MutableMap.MutableEntry<String, SortedSet<Router>>, type: AcceptMIMEType): Boolean {
        val acceptType: Array<String> = StringUtils.split(e.key, '/')
        val parentType = acceptType[0].trim()
        val childType = acceptType[1].trim()
        return when (type.matchType) {
            AcceptMIMEMatchType.EXACT -> parentType == type.parentType && childType == type.childType
            AcceptMIMEMatchType.CHILD -> childType == type.childType
            AcceptMIMEMatchType.PARENT -> parentType == type.parentType
            AcceptMIMEMatchType.ALL -> true
            else -> false
        }
    }
}