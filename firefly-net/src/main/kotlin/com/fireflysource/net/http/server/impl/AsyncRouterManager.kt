package com.fireflysource.net.http.server.impl

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
}