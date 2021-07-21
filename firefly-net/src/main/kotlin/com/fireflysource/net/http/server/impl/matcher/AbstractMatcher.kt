package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Router
import com.fireflysource.net.http.server.impl.router.AsyncRouter
import com.fireflysource.net.http.server.impl.router.AsyncRouterManager
import java.util.*

abstract class AbstractMatcher<T>(val routersMap: MutableMap<T, SortedSet<Router>> = HashMap()) {

    fun copyRouterMap(manager: AsyncRouterManager): MutableMap<T, SortedSet<Router>> {
        val newRoutersMap: MutableMap<T, SortedSet<Router>> = HashMap()
        routersMap.forEach { (key, routerSet) ->
            val newRouterSet: SortedSet<Router> = TreeSet()
            routerSet.forEach { router ->
                if (router is AsyncRouter) {
                    val newRouter = router.copy(manager)
                    newRouterSet.add(newRouter)
                }
            }
            newRoutersMap[key] = newRouterSet
        }
        return newRoutersMap
    }
}