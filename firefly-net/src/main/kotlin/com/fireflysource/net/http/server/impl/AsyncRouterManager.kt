package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.Router
import com.fireflysource.net.http.server.RouterManager
import java.util.*

class AsyncRouterManager : RouterManager {


    override fun register(): Router {
        TODO("Not yet implemented")
    }

    override fun register(id: Int): Router {
        TODO("Not yet implemented")
    }

    override fun findRouter(
        method: String,
        path: String,
        contentType: String,
        accept: String
    ): NavigableSet<RouterManager.RouterMatchResult> {
        TODO("Not yet implemented")
    }

    fun method(httpMethod: String) {
        TODO("Not yet implemented")
    }

    fun method(httpMethod: HttpMethod) {
        TODO("Not yet implemented")
    }

    fun path(url: String) {
        TODO("Not yet implemented")
    }

    fun paths(urlList: MutableList<String>) {
        TODO("Not yet implemented")
    }

    fun pathRegex(regex: String) {
        TODO("Not yet implemented")
    }

    fun get(url: String) {
        TODO("Not yet implemented")
    }

    fun post(url: String) {
        TODO("Not yet implemented")
    }

    fun put(url: String) {
        TODO("Not yet implemented")
    }

    fun delete(url: String) {
        TODO("Not yet implemented")
    }

    fun consumes(contentType: String) {
        TODO("Not yet implemented")
    }

    fun produces(accept: String) {
        TODO("Not yet implemented")
    }
}