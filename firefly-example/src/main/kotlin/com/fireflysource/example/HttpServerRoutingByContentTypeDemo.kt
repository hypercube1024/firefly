package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.common.annotation.NoArg
import com.fireflysource.net.http.common.model.HttpField
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.serialization.SerializationServiceFactory.json
import com.fireflysource.serialization.impl.json.read

@NoArg
data class Car(var name: String, var color: String)

fun main() {
    `$`.httpServer()
        .router().put("/product/:id").consumes("*/json")
        .handler { ctx ->
            val id = ctx.getPathParameter("id")
            val type = ctx.getPathParameter(0)
            val car = json().read<Car>(ctx.stringBody)

            ctx.write("Update product. id: $id, type: $type. \r\n")
                .end(car.toString())
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient()
        .put("$url/product/3")
        .add(HttpField(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value))
        .body(json().write(Car("Benz", "Black")))
        .submit().thenAccept { response -> println(response.stringBody) }
}