package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router

class AsyncRouter(
    private val id: Int,
    private val routerManager: AsyncRouterManager
) : Router {

    override fun method(httpMethod: String): Router {
        TODO("Not yet implemented")
    }

    override fun method(httpMethod: HttpMethod): Router {
        TODO("Not yet implemented")
    }

    override fun path(url: String): Router {
        TODO("Not yet implemented")
    }

    override fun paths(urlList: MutableList<String>): Router {
        TODO("Not yet implemented")
    }

    override fun pathRegex(regex: String?): Router {
        TODO("Not yet implemented")
    }

    override fun get(url: String): Router {
        TODO("Not yet implemented")
    }

    override fun post(url: String): Router {
        TODO("Not yet implemented")
    }

    override fun put(url: String): Router {
        TODO("Not yet implemented")
    }

    override fun delete(url: String): Router {
        TODO("Not yet implemented")
    }

    override fun consumes(contentType: String): Router {
        TODO("Not yet implemented")
    }

    override fun produces(accept: String): Router {
        TODO("Not yet implemented")
    }

    override fun getId(): Int = id

    override fun compareTo(other: Router): Int = id.compareTo(other.id)

    override fun handler(handler: Router.Handler): Router {
        TODO("Not yet implemented")
    }

    override fun getMatchTypes(): MutableSet<Matcher.MatchType> {
        TODO("Not yet implemented")
    }

    override fun enable(): Router {
        TODO("Not yet implemented")
    }

    override fun isEnable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun disable(): Router {
        TODO("Not yet implemented")
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