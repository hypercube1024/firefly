package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.AsyncRouter
import com.fireflysource.net.http.server.impl.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestRegexPathMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by regex path successfully.")
    fun test() {
        val matcher = RegexPathMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        val router3 = AsyncRouter(3, routerManager)

        matcher.add("/hello(\\d*)", router1)
        matcher.add("/hello(\\d*)", router2)
        matcher.add("/foo/([a-c]+)", router3)

        val result1 = matcher.match("/hello345")
        requireNotNull(result1)
        assertEquals(2, result1.routers.size)
        assertEquals(Matcher.MatchType.PATH, result1.matchType)
        assertEquals("345", result1.parameters[router2]?.get("group1"))

        val result2 = matcher.match("/foo/abccc")
        requireNotNull(result2)
        assertEquals(1, result2.routers.size)
        assertEquals(Matcher.MatchType.PATH, result2.matchType)
        assertEquals("abccc", result2.parameters[router3]?.get("group1"))

        val result3 = matcher.match("/foo/abcde")
        assertNull(result3)
    }
}