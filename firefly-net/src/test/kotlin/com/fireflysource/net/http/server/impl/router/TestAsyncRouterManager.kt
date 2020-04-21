package com.fireflysource.net.http.server.impl.router

import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.http.server.Matcher.MatchType.*
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
        routerManager.register().post("/hello")
        routerManager.register().put("/hello")
        routerManager.register().delete("/hello")

        var ctx = createContext("GET", "/hello")
        var result = routerManager.findRouters(ctx)
        assertEquals(1, result.size)
        assertEquals(0, result.first().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(METHOD, PATH)))

        ctx = createContext("POST", "/hello")
        result = routerManager.findRouters(ctx)
        assertEquals(1, result.size)
        assertEquals(1, result.first().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(METHOD, PATH)))

        ctx = createContext("PUT", "/hello")
        result = routerManager.findRouters(ctx)
        assertEquals(1, result.size)
        assertEquals(2, result.first().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(METHOD, PATH)))

        ctx = createContext("DELETE", "/hello")
        result = routerManager.findRouters(ctx)
        assertEquals(1, result.size)
        assertEquals(3, result.first().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(METHOD, PATH)))
    }

    @Test
    @DisplayName("should not find routers")
    fun testNotFound() {
        val routerManager = AsyncRouterManager(httpServer)
        routerManager.register().get("/hello")

        val ctx = createContext("POST", "/hello")
        val result1 = routerManager.findRouters(ctx)
        assertTrue(result1.isEmpty())
    }

    @Test
    @DisplayName("should find routers by paths successfully")
    fun testPaths() {
        val routerManager = AsyncRouterManager(httpServer)
        routerManager.register().paths(listOf("/hello", "/hello/*"))
        routerManager.register().paths(listOf("/hello/:name", "/foo"))

        val ctx1 = createContext("POST", "/hello")
        val result1 = routerManager.findRouters(ctx1)
        assertEquals(1, result1.size)
        assertEquals(0, result1.first().router.id)
        assertTrue(result1.first().matchTypes.containsAll(listOf(PATH)))

        val ctx2 = createContext("POST", "/hello/xxx")
        val result2 = routerManager.findRouters(ctx2)
        assertEquals(2, result2.size)
        assertTrue(result2.first().matchTypes.containsAll(listOf(PATH)))

        val ctx3 = createContext("PUT", "/foo")
        val result3 = routerManager.findRouters(ctx3)
        assertEquals(1, result3.size)
        assertEquals(1, result3.first().router.id)
        assertTrue(result3.first().matchTypes.containsAll(listOf(PATH)))
    }

    @Test
    @DisplayName("should find routers by content type successfully")
    fun testConsume() {
        val routerManager = AsyncRouterManager(httpServer)
        routerManager.register().path("/hello").consumes("*/json")
        routerManager.register().path("/hello").consumes("text/html")

        val ctx1 = createContext("GET", "/hello", "application/json")
        val result1 = routerManager.findRouters(ctx1)
        assertEquals(1, result1.size)
        assertEquals(0, result1.first().router.id)
        assertTrue(result1.first().matchTypes.containsAll(listOf(PATH, CONTENT_TYPE)))

        val ctx2 = createContext("GET", "/hello", "text/html")
        val result2 = routerManager.findRouters(ctx2)
        assertEquals(1, result2.size)
        assertEquals(1, result2.first().router.id)
        assertTrue(result1.first().matchTypes.containsAll(listOf(PATH, CONTENT_TYPE)))

        val ctx3 = createContext("POST", "/test", "text/html")
        val result3 = routerManager.findRouters(ctx3)
        assertTrue(result3.isEmpty())
    }

    @Test
    @DisplayName("should find routers by accept successfully")
    fun testAccept() {
        val routerManager = AsyncRouterManager(httpServer)
        routerManager.register().path("/*").produces("application/json")
        routerManager.register().path("/hello").produces("text/html")

        var ctx = createContext("GET", "/hello", "", "text/html,application/xml;q=0.9,application/json;q=0.8")
        var result = routerManager.findRouters(ctx)
        assertEquals(1, result.size)
        assertEquals(1, result.first().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(PATH, ACCEPT)))

        ctx = createContext("GET", "/hello", "", "text/html;q=0.6,application/xml;q=0.7,application/json;q=0.8")
        result = routerManager.findRouters(ctx)
        assertEquals(1, result.size)
        assertEquals(0, result.first().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(PATH, ACCEPT)))
    }

    @Test
    @DisplayName("should find routers by regex successfully")
    fun testRegex() {
        val routerManager = AsyncRouterManager(httpServer)
        routerManager.register(30).pathRegex("/foo/([a-c]+)")
        routerManager.register(20).path("/foo/*")

        var ctx = createContext("GET", "/foo/abc", "", "text/html,application/xml;q=0.9,application/json;q=0.8")
        var result = routerManager.findRouters(ctx)
        assertEquals(2, result.size)
        assertEquals(20, result.first().router.id)
        assertEquals(30, result.last().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(PATH)))

        ctx = createContext("GET", "/foo/ddd")
        result = routerManager.findRouters(ctx)
        assertEquals(1, result.size)
        assertEquals(20, result.first().router.id)
        assertTrue(result.first().matchTypes.containsAll(listOf(PATH)))
    }

    private fun createContext(
        method: String,
        uri: String,
        contentType: String = "",
        accept: String = ""
    ): RoutingContext {
        val ctx = Mockito.mock(RoutingContext::class.java)
        `when`(ctx.method).thenReturn(method)
        `when`(ctx.uri).thenReturn(HttpURI(uri))
        val fields = HttpFields()
        fields.put(HttpHeader.CONTENT_TYPE, contentType)
        fields.put(HttpHeader.ACCEPT, accept)
        `when`(ctx.httpFields).thenReturn(fields)
        `when`(ctx.contentType).thenReturn(contentType)
        return ctx
    }
}