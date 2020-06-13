package com.fireflysource.wechat.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * @author Pengtao Qiu
 */
object JsonUtils {

    val mapper = ObjectMapper()

    fun <T : Any> read(json: String, clazz: Class<T>): T = mapper.readValue(json, clazz)

    fun <T : Any> read(json: String, ref: TypeReference<T>): T = mapper.readValue(json, ref)

    inline fun <reified T : Any> read(json: String): T = mapper.readValue(json, object : TypeReference<T>() {})

    fun write(obj: Any): String = mapper.writeValueAsString(obj)

}