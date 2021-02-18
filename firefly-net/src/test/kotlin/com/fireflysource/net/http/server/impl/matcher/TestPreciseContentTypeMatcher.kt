package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.router.AsyncRouter
import com.fireflysource.net.http.server.impl.router.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestPreciseContentTypeMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by the precise content type successfully.")
    fun test() {
        val matcher = PreciseContentTypeMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        val router3 = AsyncRouter(3, routerManager)
        matcher.add("application/json", router1)
        matcher.add("application/json", router2)
        matcher.add("text/json", router3)

        val result1 = matcher.match("application/json;charset=utf-8")
        requireNotNull(result1)
        assertEquals(2, result1.routers.size)
        assertEquals(Matcher.MatchType.CONTENT_TYPE, result1.matchType)

        val result2 = matcher.match("text/json")
        requireNotNull(result2)
        assertEquals(1, result2.routers.size)
        assertEquals(Matcher.MatchType.CONTENT_TYPE, result2.matchType)

        val result3 = matcher.match("text/html")
        assertNull(result3)
    }
}