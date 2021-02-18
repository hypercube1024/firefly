package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.impl.router.AsyncRouter
import com.fireflysource.net.http.server.impl.router.AsyncRouterManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class TestHttpMethodMatcher {

    private val routerManager = Mockito.mock(AsyncRouterManager::class.java)

    @Test
    @DisplayName("should match router by the http method successfully.")
    fun test() {
        val matcher = HttpMethodMatcher()
        val router1 = AsyncRouter(1, routerManager)
        val router2 = AsyncRouter(2, routerManager)
        val router3 = AsyncRouter(3, routerManager)
        matcher.add(HttpMethod.GET.value, router1)
        matcher.add("get", router2)
        matcher.add("POST", router3)

        val result1 = matcher.match("GET")
        requireNotNull(result1)
        assertEquals(2, result1.routers.size)
        assertEquals(Matcher.MatchType.METHOD, result1.matchType)

        val result2 = matcher.match("get")
        requireNotNull(result2)
        assertEquals(2, result2.routers.size)
        assertEquals(Matcher.MatchType.METHOD, result2.matchType)

        val result3 = matcher.match("POST")
        requireNotNull(result3)
        assertEquals(1, result3.routers.size)
        assertEquals(Matcher.MatchType.METHOD, result3.matchType)
    }
}