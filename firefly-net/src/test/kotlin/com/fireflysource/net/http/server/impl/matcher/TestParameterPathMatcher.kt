package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.router.AsyncRouter
import com.fireflysource.net.http.server.impl.router.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestParameterPathMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by parameter path successfully.")
    fun test() {
        val matcher = ParameterPathMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        val router3 = AsyncRouter(3, routerManager)

        matcher.add("/hello/:foo", router1)
        matcher.add("/:hello/:foo/", router2)
        matcher.add("/hello/:foo/:bar", router3)

        val result1 = matcher.match("/hello/abc")
        requireNotNull(result1)
        assertEquals(2, result1.routers.size)
        assertEquals(Matcher.MatchType.PATH, result1.matchType)
        assertEquals("abc", result1.parameters[router1]?.get("foo"))
        assertEquals("abc", result1.parameters[router2]?.get("foo"))
        assertEquals("hello", result1.parameters[router2]?.get("hello"))

        val result2 = matcher.match("/hello/abc/eee/")
        requireNotNull(result2)
        assertEquals(1, result2.routers.size)
        assertEquals(Matcher.MatchType.PATH, result2.matchType)
        assertEquals("abc", result2.parameters[router3]?.get("foo"))
        assertEquals("eee", result2.parameters[router3]?.get("bar"))

        val result3 = matcher.match("/hello/abc/eee/ddd")
        assertNull(result3)
    }
}