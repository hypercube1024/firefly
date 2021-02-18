package com.fireflysource.example

import com.fireflysource.`$`

fun main() {
    `$`.httpServer()
        .router().get("/product/:id").handler { ctx ->
            val id = ctx.getPathParameter("id")
            ctx.end("Get the product $id")
        }
        .router().post("/product").handler { ctx ->
            ctx.end("Create the product 1")
        }
        .router().put("/product/:id").handler { ctx ->
            val id = ctx.getPathParameter("id")
            ctx.end("Update the product $id")
        }
        .router().delete("/product/:id").handler { ctx ->
            val id = ctx.getPathParameter("id")
            ctx.end("Delete the product $id")
        }
        .listen("localhost", 8090)
}