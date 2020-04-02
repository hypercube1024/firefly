package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.AsyncRouter
import com.fireflysource.net.http.server.impl.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestPrecisePathMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by the precise path successfully.")
    fun test() {
        val matcher = PrecisePathMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        val router3 = AsyncRouter(3, routerManager)
        val router4 = AsyncRouter(4, routerManager)
        matcher.add("/", router1)
        matcher.add("/hello", router2)
        matcher.add("/hello/", router3)
        matcher.add("/hello/foo", router4)

        val result1 = matcher.match("/")
        requireNotNull(result1)
        assertEquals(Matcher.MatchType.PATH, result1.matchType)
        assertEquals(1, result1.routers.size)
        assertEquals(1, result1.routers.first().id)

        val result2 = matcher.match("/hello")
        requireNotNull(result2)
        assertEquals(Matcher.MatchType.PATH, result2.matchType)
        assertEquals(2, result2.routers.size)

        val result3 = matcher.match("/hello/foo/")
        requireNotNull(result3)
        assertEquals(Matcher.MatchType.PATH, result3.matchType)
        assertEquals(1, result3.routers.size)
        assertEquals(4, result3.routers.first().id)

        val result4 = matcher.match("/hello/foo/bar")
        assertNull(result4)
    }
}