package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.impl.router.handler.FileHandler

fun main() {
    `$`.httpServer()
        .router().method(HttpMethod.GET)
        .paths(listOf("/favicon.ico", "/poem.html", "/poem.txt"))
        .handler(FileHandler.createFileHandlerByResourcePath("files"))
        .listen("localhost", 8090)
}