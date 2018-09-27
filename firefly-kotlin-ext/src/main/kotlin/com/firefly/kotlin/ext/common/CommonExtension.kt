package com.firefly.kotlin.ext.common

import com.firefly.`$`
import com.firefly.utils.json.JsonArray
import com.firefly.utils.json.JsonObject
import com.firefly.utils.lang.GenericTypeReference

/**
 * @author Pengtao Qiu
 */
typealias firefly = `$`

private typealias json = `$`.json

object Json {
    fun toJson(obj: Any): String = json.toJson(obj)

    fun parseToObject(jsonString: String): JsonObject = json.parseToObject(jsonString)

    fun parseToArray(jsonString: String): JsonArray = json.parseToArray(jsonString)

    inline fun <reified T : Any> parse(str: String): T = json.parse(str, object : GenericTypeReference<T>() {})
}

