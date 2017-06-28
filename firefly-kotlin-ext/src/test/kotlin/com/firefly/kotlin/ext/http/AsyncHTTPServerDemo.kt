package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.common.Json
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.log.Log

/**
 * @author Pengtao Qiu
 */

private val log = Log.getLogger { }

fun main(args: Array<String>) {
    firefly.httpServer().router().get("/good/:type/:id").asyncHandler {
        val type = getRouterParameter("type")
        val id = getRouterParameter("id")
        log.info("req type: $type, id: $id")
        end("get good type: $type, id: $id")
    }.router().get("/jsonMsg").asyncHandler {
        put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString())
        end(Json.toJson(JsonMsg("fuck xxx", 33)))
    }.listen("localhost", 8080)
}

@NoArg
data class JsonMsg(var msg: String, var code: Int)