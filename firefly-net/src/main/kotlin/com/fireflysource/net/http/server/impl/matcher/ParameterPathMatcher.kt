package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import java.util.*


class ParameterPathMatcher : AbstractMatcher<ParameterPathMatcher.ParameterRule>(), Matcher {

    companion object {
        fun isParameterPath(path: String): Boolean {
            val paths = split(path)
            return paths.any { it[0] == ':' }
        }

        fun split(path: String): List<String> {
            val paths: MutableList<String> = LinkedList()
            var start = 1
            val last = path.lastIndex

            for (i in 1..last) {
                if (path[i] == '/') {
                    paths.add(path.substring(start, i).trim())
                    start = i + 1
                }
            }

            if (path[last] != '/') {
                paths.add(path.substring(start).trim())
            }
            return paths
        }
    }

    inner class ParameterRule(val rule: String) {

        val paths = split(rule)

        fun match(list: List<String>): Map<String, String> {
            if (paths.size != list.size) return emptyMap()

            val param: MutableMap<String, String> = HashMap()
            for (i in list.indices) {
                val path = paths[i]
                val value = list[i]
                if (path[0] != ':') {
                    if (path != value) {
                        return emptyMap()
                    }
                } else {
                    param[path.substring(1)] = value
                }
            }
            return param
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParameterRule
            return rule == other.rule
        }

        override fun hashCode(): Int {
            return rule.hashCode()
        }
    }

    override fun getMatchType(): Matcher.MatchType {
        return Matcher.MatchType.PATH
    }

    override fun add(rule: String, router: Router) {
        val parameterRule = ParameterRule(rule)
        routersMap.computeIfAbsent(parameterRule) { TreeSet() }.add(router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        if (routersMap.isEmpty()) return null

        val routers = TreeSet<Router>()
        val parameters = HashMap<Router, Map<String, String>>()
        val paths = split(value)

        routersMap.forEach { (rule, routerSet) ->
            val param = rule.match(paths)
            if (param.isNotEmpty()) {
                routers.addAll(routerSet)
                routerSet.forEach { router -> parameters[router] = param }
            }
        }
        return if (routers.isEmpty()) null else Matcher.MatchResult(routers, parameters, matchType)
    }
}