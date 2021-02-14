package com.fireflysource.example

import com.fireflysource.`$`

fun main() {
    `$`.httpServer()
        .router().get("/").handler { ctx ->
            ctx.attributes["router1"] = "Some one visits the /. "
            ctx.write("Hello world! ").next()
        }
        .router().get("/").handler { ctx ->
            val data = ctx.attributes["router1"]
            ctx.end("The router data: $data")
        }
        .listen("localhost", 8090)
}