package com.fireflysource.net.http.server.impl.router

import com.fireflysource.net.http.common.codec.URIUtils.canonicalPath
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.server.*
import com.fireflysource.net.http.server.impl.matcher.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AsyncRouterManager(val httpServer: HttpServer) : RouterManager {

    private val routerId = AtomicInteger()

    private val httpMethodMatcher: HttpMethodMatcher = HttpMethodMatcher()
    private val precisePathMatcher: PrecisePathMatcher = PrecisePathMatcher()
    private val parameterPathMatcher: ParameterPathMatcher = ParameterPathMatcher()
    private val patternedPathMatcher: PatternedPathMatcher = PatternedPathMatcher()
    private val regexPathMatcher: RegexPathMatcher = RegexPathMatcher()
    private val preciseContentTypeMatcher: PreciseContentTypeMatcher = PreciseContentTypeMatcher()
    private val patternedContentTypeMatcher: PatternedContentTypeMatcher = PatternedContentTypeMatcher()
    private val acceptHeaderMatcher: AcceptHeaderMatcher = AcceptHeaderMatcher()

    override fun register(): Router = this.register(routerId.getAndIncrement())

    override fun register(id: Int): Router =
        AsyncRouter(id, this)

    override fun findRouters(ctx: RoutingContext): SortedSet<RouterManager.RouterMatchResult> {
        val routerMatchTypeMap = TreeMap<Router, MutableSet<Matcher.MatchType>>()
        val routerParameterMap = TreeMap<Router, MutableMap<String, String>>()

        val methodResult = httpMethodMatcher.match(ctx.method)
        collectRouterResult(methodResult, routerMatchTypeMap, routerParameterMap)

        val path = canonicalPath(ctx.uri.decodedPath)
        val precisePathResult = precisePathMatcher.match(path)
        collectRouterResult(precisePathResult, routerMatchTypeMap, routerParameterMap)

        val parameterPathResult = parameterPathMatcher.match(path)
        collectRouterResult(parameterPathResult, routerMatchTypeMap, routerParameterMap)

        val patternedPathResult = patternedPathMatcher.match(path)
        collectRouterResult(patternedPathResult, routerMatchTypeMap, routerParameterMap)

        val regexPathResult = regexPathMatcher.match(path)
        collectRouterResult(regexPathResult, routerMatchTypeMap, routerParameterMap)

        val contentType = ctx.contentType ?: ""
        val preciseContentTypeResult = preciseContentTypeMatcher.match(contentType)
        collectRouterResult(preciseContentTypeResult, routerMatchTypeMap, routerParameterMap)

        val patternedContentTypeResult = patternedContentTypeMatcher.match(contentType)
        collectRouterResult(patternedContentTypeResult, routerMatchTypeMap, routerParameterMap)

        val accept = ctx.httpFields[HttpHeader.ACCEPT] ?: ""
        val acceptHeaderResult = acceptHeaderMatcher.match(accept)
        collectRouterResult(acceptHeaderResult, routerMatchTypeMap, routerParameterMap)

        return routerMatchTypeMap
            .filter { it.key.isEnable }
            .filter { it.key.matchTypes == it.value }
            .map { RouterManager.RouterMatchResult(it.key, routerParameterMap[it.key] ?: emptyMap(), it.value) }
            .toSortedSet()
    }

    private fun collectRouterResult(
        result: Matcher.MatchResult?,
        routerMatchTypeMap: SortedMap<Router, MutableSet<Matcher.MatchType>>,
        routerParameterMap: SortedMap<Router, MutableMap<String, String>>
    ) {
        result?.routers?.forEach {
            routerMatchTypeMap.computeIfAbsent(it) { HashSet<Matcher.MatchType>() }.add(result.matchType)
            val params = result.parameters[it]
            if (!params.isNullOrEmpty()) {
                routerParameterMap.computeIfAbsent(it) { HashMap<String, String>() }.putAll(params)
            }
        }
    }

    fun method(httpMethod: String, router: AsyncRouter) {
        httpMethodMatcher.add(httpMethod, router)
    }

    fun path(url: String, router: AsyncRouter) {
        when {
            url == "/" -> precisePathMatcher.add(url, router)
            url.contains("*") -> patternedPathMatcher.add(url, router)
            ParameterPathMatcher.isParameterPath(url) -> parameterPathMatcher.add(url, router)
            else -> precisePathMatcher.add(url, router)
        }
    }

    fun paths(urlList: MutableList<String>, router: AsyncRouter) {
        urlList.forEach { path(it, router) }
    }

    fun pathRegex(regex: String, router: AsyncRouter) {
        regexPathMatcher.add(regex, router)
    }

    fun consumes(contentType: String, router: AsyncRouter) {
        if (contentType.contains("*")) patternedContentTypeMatcher.add(contentType, router)
        else preciseContentTypeMatcher.add(contentType, router)
    }

    fun produces(accept: String, router: AsyncRouter) {
        acceptHeaderMatcher.add(accept, router)
    }

    fun copy(httpServer: HttpServer): AsyncRouterManager {
        val newManager = AsyncRouterManager(httpServer)
        newManager.routerId.set(this.routerId.get())
        newManager.httpMethodMatcher.routersMap.putAll(this.httpMethodMatcher.copyRouterMap(newManager))
        newManager.precisePathMatcher.routersMap.putAll(this.precisePathMatcher.copyRouterMap(newManager))
        newManager.parameterPathMatcher.routersMap.putAll(this.parameterPathMatcher.copyRouterMap(newManager))
        newManager.patternedPathMatcher.routersMap.putAll(this.patternedPathMatcher.copyRouterMap(newManager))
        newManager.regexPathMatcher.routersMap.putAll(this.regexPathMatcher.copyRouterMap(newManager))
        newManager.preciseContentTypeMatcher.routersMap.putAll(this.preciseContentTypeMatcher.copyRouterMap(newManager))
        newManager.patternedContentTypeMatcher.routersMap.putAll(
            this.patternedContentTypeMatcher.copyRouterMap(
                newManager
            )
        )
        newManager.acceptHeaderMatcher.routersMap.putAll(this.acceptHeaderMatcher.copyRouterMap(newManager))
        return newManager
    }
}