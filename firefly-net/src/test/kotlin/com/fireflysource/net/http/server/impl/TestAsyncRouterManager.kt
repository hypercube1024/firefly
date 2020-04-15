package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.http.server.Matcher.MatchType.METHOD
import com.fireflysource.net.http.server.Matcher.MatchType.PATH
import com.fireflysource.net.http.server.RoutingContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class TestAsyncRouterManager {

    private val httpServer: HttpServer = Mockito.mock(HttpServer::class.java)

    @Test
    @DisplayName("should find routers")
    fun test() {
        val routerManager = AsyncRouterManager(httpServer)
        routerManager.register().get("/hello")

        val ctx = Mockito.mock(RoutingContext::class.java)
        `when`(ctx.method).thenReturn("GET")
        val uri = HttpURI("/hello")
        `when`(ctx.uri).thenReturn(uri)
        `when`(ctx.httpFields).thenReturn(HttpFields())

        val result1 = routerManager.findRouters(ctx)
        assertEquals(1, result1.size)
        assertEquals(0, result1.first().router.id)
        assertTrue(result1.first().matchTypes.containsAll(listOf(METHOD, PATH)))
    }

    @Test
    @DisplayName("should not find routers")
    fun testNotFound() {
        val routerManager = AsyncRouterManager(httpServer)
        routerManager.register().get("/hello")

        val ctx = Mockito.mock(RoutingContext::class.java)
        `when`(ctx.method).thenReturn("POST")
        val uri = HttpURI("/hello")
        `when`(ctx.uri).thenReturn(uri)
        `when`(ctx.httpFields).thenReturn(HttpFields())

        val result1 = routerManager.findRouters(ctx)
        assertTrue(result1.isEmpty())
    }
}