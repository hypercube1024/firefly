package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import com.fireflysource.net.http.server.Router.EMPTY_HANDLER

class AsyncRouter(
    private val id: Int,
    private val routerManager: AsyncRouterManager
) : Router {

    private val matchTypes = HashSet<Matcher.MatchType>()
    private var handler: Router.Handler = EMPTY_HANDLER
    private var enabled = true

    override fun method(httpMethod: String): Router {
        routerManager.method(httpMethod)
        return this
    }

    override fun method(httpMethod: HttpMethod): Router {
        routerManager.method(httpMethod)
        return this
    }

    override fun path(url: String): Router {
        routerManager.path(url)
        return this
    }

    override fun paths(urlList: MutableList<String>): Router {
        routerManager.paths(urlList)
        return this
    }

    override fun pathRegex(regex: String): Router {
        routerManager.pathRegex(regex)
        return this
    }

    override fun get(url: String): Router {
        routerManager.get(url)
        return this
    }

    override fun post(url: String): Router {
        routerManager.post(url)
        return this
    }

    override fun put(url: String): Router {
        routerManager.put(url)
        return this
    }

    override fun delete(url: String): Router {
        routerManager.delete(url)
        return this
    }

    override fun consumes(contentType: String): Router {
        routerManager.consumes(contentType)
        return this
    }

    override fun produces(accept: String): Router {
        routerManager.produces(accept)
        return this
    }

    override fun getId(): Int = id

    override fun compareTo(other: Router): Int = id.compareTo(other.id)

    override fun handler(handler: Router.Handler): Router {
        this.handler = handler
        return this
    }

    override fun getMatchTypes(): MutableSet<Matcher.MatchType> = matchTypes

    override fun enable(): Router {
        enabled = true
        return this
    }

    override fun isEnable(): Boolean = enabled

    override fun disable(): Router {
        enabled = false
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AsyncRouter
        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

}