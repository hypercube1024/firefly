package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.common.annotation.NoArg
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.resourceFileBody
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.stringBody
import java.nio.file.StandardOpenOption

@NoArg
data class Product(var id: String, var brand: String, var description: String)

fun main() {
    `$`.httpServer()
        .router().post("/product/file-upload").handler { ctx ->
            val id = ctx.getPart("id")
            val brand = ctx.getPart("brand")
            val description = ctx.getPart("description")
            ctx.end(Product(id.stringBody, brand.stringBody, description.stringBody).toString())
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/product/file-upload")
        .addPart("id", stringBody("x01"), null)
        .addPart("brand", stringBody("Test"), null)
        .addFilePart(
            "description", "poem.txt",
            resourceFileBody("files/poem.txt", StandardOpenOption.READ),
            null
        )
        .submit().thenAccept { response -> println(response) }
}