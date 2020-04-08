package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.AsyncRouter
import com.fireflysource.net.http.server.impl.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestPatternedContentTypeMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by content type successfully.")
    fun test() {
        val matcher = PatternedContentTypeMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        matcher.add("*/json", router1)
        matcher.add("*/json", router2)

        val result = matcher.match("application/json;charset=utf-8")
        requireNotNull(result)
        assertEquals(2, result.routers.size)
        assertEquals(Matcher.MatchType.CONTENT_TYPE, result.matchType)
        assertEquals("application", result.parameters[router1]?.get("param0"))
        assertEquals("application", result.parameters[router2]?.get("param0"))

        val result2 = matcher.match("text/html")
        assertNull(result2)
    }
}