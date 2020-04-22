package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import java.util.*
import java.util.regex.Pattern


abstract class AbstractRegexMatcher : Matcher {

    companion object {
        const val paramName = "group"
    }

    protected val regexMap: MutableMap<RegexRule, SortedSet<Router>> = HashMap<RegexRule, SortedSet<Router>>()

    class RegexRule(val rule: String) {
        val pattern = Pattern.compile(rule)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RegexRule
            return rule == other.rule
        }

        override fun hashCode(): Int {
            return rule.hashCode()
        }
    }

    override fun add(rule: String, router: Router) {
        regexMap.computeIfAbsent(RegexRule(rule)) { TreeSet() }.add(router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        if (regexMap.isEmpty()) return null

        val routers = TreeSet<Router>()
        val parameters = HashMap<Router, Map<String, String>>()

        regexMap.forEach { (rule, routerSet) ->
            var matcher = rule.pattern.matcher(value)
            if (matcher.matches()) {
                routers.addAll(routerSet)
                matcher = rule.pattern.matcher(value)
                val param: MutableMap<String, String> = HashMap()
                while (matcher.find()) {
                    for (i in 1..matcher.groupCount()) {
                        param["$paramName$i"] = matcher.group(i)
                    }
                }
                if (param.isNotEmpty()) {
                    routerSet.forEach { router -> parameters[router] = param }
                }
            }
        }

        return if (routers.isEmpty()) null else Matcher.MatchResult(routers, parameters, matchType)
    }
}