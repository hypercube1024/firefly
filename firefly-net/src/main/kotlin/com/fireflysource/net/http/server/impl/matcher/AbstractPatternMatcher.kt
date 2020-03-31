package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.common.string.Pattern
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import java.util.*


abstract class AbstractPatternMatcher : Matcher {

    protected val patternMap: MutableMap<PatternRule, MutableSet<Router>> by lazy { HashMap<PatternRule, MutableSet<Router>>() }

    protected data class PatternRule(val rule: String) {
        val pattern: Pattern = Pattern.compile(rule, "*")
    }

    override fun add(rule: String, router: Router) {
        patternMap.computeIfAbsent(PatternRule(rule)) { HashSet() }.add(router)
    }

    override fun match(value: String): Matcher.MatchResult? {
        val routers: MutableSet<Router> = HashSet()
        val parameters: MutableMap<Router, Map<String, String>> = HashMap()

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