package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.router.AsyncRouter
import com.fireflysource.net.http.server.impl.router.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestAcceptHeaderMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by the accept-type successfully.")
    fun test() {
        val matcher = AcceptHeaderMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        val router3 = AsyncRouter(3, routerManager)
        matcher.add("application/json", router1)
        matcher.add("application/json", router2)
        matcher.add("text/html", router3)

        val result1 = matcher.match("text/html,application/xml;q=0.9,application/json;q=0.8")
        requireNotNull(result1)
        assertEquals(1, result1.routers.size)
        assertEquals(Matcher.MatchType.ACCEPT, result1.matchType)

        val result2 = matcher.match("text/html;q=0.6,application/xml;q=0.7,application/json;q=0.8")
        requireNotNull(result2)
        assertEquals(2, result2.routers.size)
        assertEquals(Matcher.MatchType.ACCEPT, result2.matchType)

        val result3 = matcher.match("text/html,application/xml,application/json")
        requireNotNull(result3)
        assertEquals(1, result3.routers.size)
        assertEquals(Matcher.MatchType.ACCEPT, result3.matchType)
    }
}