package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.router.AsyncRouter
import com.fireflysource.net.http.server.impl.router.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestPatternedPathMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by the path successfully.")
    fun test() {
        val matcher = PatternedPathMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        val router3 = AsyncRouter(3, routerManager)
        val router4 = AsyncRouter(4, routerManager)
        matcher.add("*", router1)
        matcher.add("/*", router2)
        matcher.add("/he*/*", router3)
        matcher.add("/hello*", router4)

        val result1 = matcher.match("/test")
        requireNotNull(result1)
        assertEquals(Matcher.MatchType.PATH, result1.matchType)
        assertEquals(2, result1.routers.size)
        assertEquals("/test", result1.parameters[router1]?.get("param0"))
        assertEquals("test", result1.parameters[router2]?.get("param0"))

        val result2 = matcher.match("/hello/1")
        requireNotNull(result2)
        assertEquals(Matcher.MatchType.PATH, result2.matchType)
        assertEquals(4, result2.routers.size)
        assertEquals("llo", result2.parameters[router3]?.get("param0"))
        assertEquals("1", result2.parameters[router3]?.get("param1"))
        assertEquals("/1", result2.parameters[router4]?.get("param0"))
    }

}