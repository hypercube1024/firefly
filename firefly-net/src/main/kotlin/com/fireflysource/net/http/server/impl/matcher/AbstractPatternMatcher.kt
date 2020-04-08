package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.common.string.Pattern
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import java.util.*


abstract class AbstractPatternMatcher : Matcher {

    private val patternMap: MutableMap<PatternRule, NavigableSet<Router>> by lazy { HashMap<PatternRule, NavigableSet<Router>>() }

    private class PatternRule(val rule: String) {

        val pattern: Pattern = Pattern.compile(rule, "*")

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PatternRule
            return rule == other.rule
        }

        override fun hashCode(): Int {
            return rule.hashCode()
        }
    }

    override fun add(rule: String, router: Router) {
        patternMap.computeIfAbsent(PatternRule(rule)) { TreeSet() }.add(router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        val routers = TreeSet<Router>()
        val parameters = HashMap<Router, Map<String, String>>()

        patternMap.forEach { (rule, routerSet) ->
            val strings: Array<String>? = rule.pattern.match(value)
            if (strings != null) {
                routers.addAll(routerSet)
                if (strings.isNotEmpty()) {
                    val param: MutableMap<String, String> = HashMap()
                    for (i in strings.indices) {
                        param["param$i"] = strings[i]
                    }
                    routerSet.forEach { router -> parameters[router] = param }
                }
            }
        }
        return if (routers.isEmpty()) null else Matcher.MatchResult(routers, parameters, matchType)
    }
}