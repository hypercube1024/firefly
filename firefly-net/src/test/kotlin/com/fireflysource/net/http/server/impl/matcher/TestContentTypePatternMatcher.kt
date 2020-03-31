package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.AsyncRouter
import com.fireflysource.net.http.server.impl.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestContentTypePatternMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by content type successfully.")
    fun test() {
        val p = ContentTypePatternMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        p.add("*/json", router1)
        p.add("*/json", router2)

        val result = p.match("application/json")
        assertNotNull(result)
        requireNotNull(result)
        assertEquals(2, result.routers.size)
        assertEquals(Matcher.MatchType.CONTENT_TYPE, result.matchType)
        assertEquals("application", result.parameters[router1]?.get("param0"))
        assertEquals("application", result.parameters[router2]?.get("param0"))
    }
}